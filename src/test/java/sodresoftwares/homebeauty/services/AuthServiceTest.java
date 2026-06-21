package sodresoftwares.homebeauty.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import sodresoftwares.homebeauty.dto.AuthenticationDTO;
import sodresoftwares.homebeauty.dto.LoginResponseDTO;
import sodresoftwares.homebeauty.dto.RegisterDTO;
import sodresoftwares.homebeauty.dto.VerifyCodeDTO;
import sodresoftwares.homebeauty.infra.exception.AppException;
import sodresoftwares.homebeauty.infra.security.TokenService;
import sodresoftwares.homebeauty.model.ProfessionalProfile;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.model.user.UserRole;
import sodresoftwares.homebeauty.repositories.ProfessionalProfileRepository;
import sodresoftwares.homebeauty.repositories.UserRepository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService
 *
 * Tests business logic for user authentication, registration, and role management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ProfessionalProfileRepository profileRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private AuthenticationDTO authenticationDTO;
    private RegisterDTO registerDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-123")
                .login("user@test.com")
                .password("encrypted-password")
                .firstName("John")
                .lastName("")
                .phone("11999999999")
                .cpf("12345678900")
                .birthDate(java.time.LocalDate.of(1990, 1, 1))
                .gender("M")
                .role(UserRole.USER)
                .build();

        authenticationDTO = new AuthenticationDTO("user@test.com", "password123");
        registerDTO = new RegisterDTO("newuser@test.com", "password123", "New", "User", UserRole.USER);
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("Should login USER with complete profile and return isProfileComplete true")
    void testLogin_UserWithCompleteProfile() {
        // Arrange
        UsernamePasswordAuthenticationToken authenticatedToken = new UsernamePasswordAuthenticationToken(
                testUser, null, testUser.getAuthorities()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticatedToken);
        when(tokenService.generateToken(testUser))
                .thenReturn("jwt-token-user");

        // Act
        LoginResponseDTO result = authService.login(new AuthenticationDTO("user@test.com", "password123"));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.token()).isEqualTo("jwt-token-user");
        assertThat(result.role()).isEqualTo(UserRole.USER);
        assertThat(result.isProfileComplete()).isTrue();

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService).generateToken(testUser);
    }

    @Test
    @DisplayName("Should login USER with incomplete profile and return isProfileComplete false")
    void testLogin_UserWithIncompleteProfile() {
        // Arrange
        User incompleteUser = User.builder()
                .id("user-123")
                .login("user@test.com")
                .role(UserRole.USER)
                .phone("11999999999")
                .build();

        UsernamePasswordAuthenticationToken authenticatedToken = new UsernamePasswordAuthenticationToken(
                incompleteUser, null, incompleteUser.getAuthorities()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticatedToken);
        when(tokenService.generateToken(incompleteUser))
                .thenReturn("jwt-token-user");

        // Act
        LoginResponseDTO result = authService.login(new AuthenticationDTO("user@test.com", "password123"));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.token()).isEqualTo("jwt-token-user");
        assertThat(result.role()).isEqualTo(UserRole.USER);
        assertThat(result.isProfileComplete()).isFalse();
    }

    @Test
    @DisplayName("Should login ADMIN and always return isProfileComplete true")
    void testLogin_Admin() {
        // Arrange
        User adminUser = User.builder()
                .id("admin-123")
                .login("admin@test.com")
                .role(UserRole.ADMIN)
                .build();

        UsernamePasswordAuthenticationToken authenticatedToken = new UsernamePasswordAuthenticationToken(
                adminUser, null, adminUser.getAuthorities()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticatedToken);
        when(tokenService.generateToken(adminUser))
                .thenReturn("jwt-token-admin");

        // Act
        LoginResponseDTO result = authService.login(new AuthenticationDTO("admin@test.com", "password123"));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.token()).isEqualTo("jwt-token-admin");
        assertThat(result.role()).isEqualTo(UserRole.ADMIN);
        assertThat(result.isProfileComplete()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when credentials are invalid")
    void testLogin_InvalidCredentials() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(authenticationDTO))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Should login PROFESSIONAL and return isProfileComplete true")
    void testLogin_ProfessionalWithCompleteProfile() {
        // Arrange
        User professionalUser = User.builder()
                .id("prof-123")
                .login("pro@test.com")
                .role(UserRole.PROFESSIONAL)
                .build();

        UsernamePasswordAuthenticationToken authenticatedToken = new UsernamePasswordAuthenticationToken(
                professionalUser, null, professionalUser.getAuthorities()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticatedToken);
        when(tokenService.generateToken(professionalUser))
                .thenReturn("jwt-token-pro");
        when(profileRepository.findByUserId("prof-123"))
                .thenReturn(Optional.of(new ProfessionalProfile()));

        // Act
        LoginResponseDTO result = authService.login(new AuthenticationDTO("pro@test.com", "password123"));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.token()).isEqualTo("jwt-token-pro");
        assertThat(result.role()).isEqualTo(UserRole.PROFESSIONAL);
        assertThat(result.isProfileComplete()).isTrue();

        verify(profileRepository).findByUserId("prof-123");
    }

    @Test
    @DisplayName("Should login PROFESSIONAL and return isProfileComplete false")
    void testLogin_ProfessionalWithIncompleteProfile() {
        // Arrange
        User professionalUser = User.builder()
                .id("prof-123")
                .login("pro@test.com")
                .role(UserRole.PROFESSIONAL)
                .build();

        UsernamePasswordAuthenticationToken authenticatedToken = new UsernamePasswordAuthenticationToken(
                professionalUser, null, professionalUser.getAuthorities()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticatedToken);
        when(tokenService.generateToken(professionalUser))
                .thenReturn("jwt-token-pro");
        when(profileRepository.findByUserId("prof-123"))
                .thenReturn(Optional.empty());

        // Act
        LoginResponseDTO result = authService.login(new AuthenticationDTO("pro@test.com", "password123"));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.token()).isEqualTo("jwt-token-pro");
        assertThat(result.role()).isEqualTo(UserRole.PROFESSIONAL);
        assertThat(result.isProfileComplete()).isFalse();

        verify(profileRepository).findByUserId("prof-123");
    }

    // ==================== REGISTER TESTS ====================

    @Test
    @DisplayName("Should register new user successfully")
    void testRegister_Success() {
        // Arrange
        when(userRepository.existsByLogin(registerDTO.login())).thenReturn(false);
        when(passwordEncoder.encode(registerDTO.password())).thenReturn("encrypted-password");

        // Act
        authService.register(registerDTO);

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();

        // Assert basic fields
        assertThat(savedUser.getLogin()).isEqualTo("newuser@test.com");
        assertThat(savedUser.getFirstName()).isEqualTo("New");
        assertThat(savedUser.getLastName()).isEqualTo("User");
        assertThat(savedUser.getPassword()).isEqualTo("encrypted-password");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);

        // Assert critical business rules
        assertThat(savedUser.isActive()).isFalse(); // Account must be created disabled
        assertThat(savedUser.getVerificationCode()).isNotNull(); // Code must be generated
        assertThat(savedUser.getVerificationCodeExpiry()).isNotNull(); // Expiry must be set

        // Verify interactions
        verify(userRepository).existsByLogin("newuser@test.com");
        verify(passwordEncoder).encode("password123");

        // Verify email was triggered with the generated code
        verify(emailService).sendVerificationCode(eq("newuser@test.com"), eq("New"), eq(savedUser.getVerificationCode()));
    }

    @Test
    @DisplayName("Should throw CONFLICT when user already exists")
    void testRegister_UserAlreadyExists() {
        // Arrange
        when(userRepository.existsByLogin(registerDTO.login())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasFieldOrPropertyWithValue("errorCode", "USER_ALREADY_EXISTS")
                .hasMessage("User already exists");

        // Verify interactions
        verify(userRepository).existsByLogin("newuser@test.com");
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(emailService, never()).sendVerificationCode(anyString(), anyString(), anyString());
    }

    // ==================== VERIFY ACCOUNT TESTS ====================

    @Test
    @DisplayName("Should verify account successfully when code matches and not expired")
    void testVerifyAccount_Success() {
        // Arrange
        User pending = User.builder()
                .id("pending-1")
                .login("pending@test.com")
                .firstName("first")
                .lastName("User")
                .isActive(false)
                .verificationCode("654321")
                .verificationCodeExpiry(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5))
                .role(UserRole.USER)
                .build();

        when(userRepository.findByLogin("pending@test.com")).thenReturn(pending);

        // Act
        authService.verifyAccount(new VerifyCodeDTO("pending@test.com", "654321"));

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getVerificationCode()).isNull();
        assertThat(saved.getVerificationCodeExpiry()).isNull();
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when verifying non-existent account")
    void testVerifyAccount_UserNotFound() {
        when(userRepository.findByLogin("missing@test.com")).thenReturn(null);

        assertThatThrownBy(() -> authService.verifyAccount(new VerifyCodeDTO("missing@test.com", "000000")))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "USER_NOT_FOUND")
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when verifying an already active account")
    void testVerifyAccount_AlreadyActive() {
        User active = User.builder().id("a1").login("active@test.com").isActive(true).build();
        when(userRepository.findByLogin("active@test.com")).thenReturn(active);

        assertThatThrownBy(() -> authService.verifyAccount(new VerifyCodeDTO("active@test.com", "111111")))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "ACCOUNT_ALREADY_ACTIVE")
                .hasMessage("Account is already active");
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when verification code is invalid")
    void testVerifyAccount_InvalidCode() {
        User pending = User.builder()
                .id("p2")
                .login("pending2@test.com")
                .isActive(false)
                .verificationCode("222222")
                .verificationCodeExpiry(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5))
                .build();
        when(userRepository.findByLogin("pending2@test.com")).thenReturn(pending);

        assertThatThrownBy(() -> authService.verifyAccount(new VerifyCodeDTO("pending2@test.com", "999999")))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "INVALID_VERIFICATION_CODE")
                .hasMessage("Invalid verification code");
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when verification code expired")
    void testVerifyAccount_Expired() {
        User pending = User.builder()
                .id("p3")
                .login("pending3@test.com")
                .isActive(false)
                .verificationCode("333333")
                .verificationCodeExpiry(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1))
                .build();
        when(userRepository.findByLogin("pending3@test.com")).thenReturn(pending);

        assertThatThrownBy(() -> authService.verifyAccount(new VerifyCodeDTO("pending3@test.com", "333333")))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "EXPIRED_VERIFICATION_CODE")
                .hasMessage("Verification code expired");
    }

    // ==================== RESEND VERIFICATION CODE TESTS ====================

    @Test
    @DisplayName("Should resend verification code and send email when user exists and not active")
    void testResendVerificationCode_Success() {
        User pending = User.builder()
                .id("r1")
                .login("resend@test.com")
                .firstName("Resend")
                .lastName("User")
                .isActive(false)
                .build();

        when(userRepository.findByLogin("resend@test.com")).thenReturn(pending);

        authService.resendVerificationCode("resend@test.com");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getVerificationCode()).isNotNull();
        assertThat(saved.getVerificationCodeExpiry()).isNotNull();
        assertThat(saved.getVerificationCodeExpiry()).isAfter(LocalDateTime.now(ZoneOffset.UTC));

        verify(emailService).sendVerificationCode(
                eq("resend@test.com"),
                eq("Resend"),
                eq(saved.getVerificationCode())
        );
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when resending code for non-existent user")
    void testResendVerificationCode_UserNotFound() {
        when(userRepository.findByLogin("no-user@test.com")).thenReturn(null);

        assertThatThrownBy(() -> authService.resendVerificationCode("no-user@test.com"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "USER_NOT_FOUND")
                .hasMessage("User not found.");
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when resending code for already active account")
    void testResendVerificationCode_AlreadyActive() {
        User active = User.builder().id("r2").login("already@test.com").isActive(true).build();
        when(userRepository.findByLogin("already@test.com")).thenReturn(active);

        assertThatThrownBy(() -> authService.resendVerificationCode("already@test.com"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "ACCOUNT_ALREADY_ACTIVE")
                .hasMessage("This account is already activated.");
    }

    // ==================== PROMOTE TO ADMIN TESTS ====================

    @Test
    @DisplayName("Should promote user to admin successfully")
    void testPromoteToAdmin_Success() {
        // Arrange
        User userToPromote = User.builder()
                .id("user-123")
                .login("user@test.com")
                .role(UserRole.USER)
                .build();

        when(userRepository.findById("user-123")).thenReturn(Optional.of(userToPromote));
        when(userRepository.save(any(User.class))).thenReturn(userToPromote);

        // Act
        authService.promoteToAdmin("user-123");

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();
        assertThat(savedUser.getRole()).isEqualTo(UserRole.ADMIN);

        verify(userRepository).findById("user-123");
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when promoting non-existent user")
    void testPromoteToAdmin_UserNotFound() {
        // Arrange
        when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.promoteToAdmin("non-existent"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "USER_NOT_FOUND")
                .hasMessage("User not found");

        verify(userRepository).findById("non-existent");
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== DEMOTE FROM ADMIN TESTS ====================

    @Test
    @DisplayName("Should demote admin user to USER when not a professional")
    void testDemoteFromAdmin_UserWithoutProfessionalProfile() {
        // Arrange
        User adminUser = User.builder()
                .id("admin-123")
                .login("admin@test.com")
                .role(UserRole.ADMIN)
                .build();

        when(userRepository.findById("admin-123")).thenReturn(Optional.of(adminUser));
        when(profileRepository.findByUserId("admin-123")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(adminUser);

        // Act
        authService.demoteFromAdmin("admin-123");

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);

        verify(userRepository).findById("admin-123");
        verify(profileRepository).findByUserId("admin-123");
    }

    @Test
    @DisplayName("Should demote admin user to PROFESSIONAL when professional profile exists")
    void testDemoteFromAdmin_UserWithProfessionalProfile() {
        // Arrange
        User adminProfessional = User.builder()
                .id("admin-prof-123")
                .login("admin-prof@test.com")
                .role(UserRole.ADMIN)
                .build();

        ProfessionalProfile professionalProfile = ProfessionalProfile.builder()
                .id("prof-profile-123")
                .user(adminProfessional)
                .build();

        when(userRepository.findById("admin-prof-123")).thenReturn(Optional.of(adminProfessional));
        when(profileRepository.findByUserId("admin-prof-123")).thenReturn(Optional.of(professionalProfile));
        when(userRepository.save(any(User.class))).thenReturn(adminProfessional);

        // Act
        authService.demoteFromAdmin("admin-prof-123");

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();
        assertThat(savedUser.getRole()).isEqualTo(UserRole.PROFESSIONAL);

        verify(userRepository).findById("admin-prof-123");
        verify(profileRepository).findByUserId("admin-prof-123");
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when demoting non-existent user")
    void testDemoteFromAdmin_UserNotFound() {
        // Arrange
        when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.demoteFromAdmin("non-existent"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "USER_NOT_FOUND")
                .hasMessage("User not found");

        verify(userRepository).findById("non-existent");
        verify(profileRepository, never()).findByUserId(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}