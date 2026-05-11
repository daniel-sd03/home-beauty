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
import org.springframework.web.server.ResponseStatusException;
import sodresoftwares.homebeauty.dto.AuthenticationDTO;
import sodresoftwares.homebeauty.dto.LoginResponseDTO;
import sodresoftwares.homebeauty.dto.RegisterDTO;
import sodresoftwares.homebeauty.infra.security.TokenService;
import sodresoftwares.homebeauty.model.ProfessionalProfile;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.model.user.UserRole;
import sodresoftwares.homebeauty.repositories.ProfessionalProfileRepository;
import sodresoftwares.homebeauty.repositories.UserRepository;

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
                .name("John Doe")
                .phone("11999999999")
                .role(UserRole.USER)
                .build();

        authenticationDTO = new AuthenticationDTO("user@test.com", "password123");
        registerDTO = new RegisterDTO("newuser@test.com", "password123", "New User", "11988888888");
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("Should login successfully and return token")
    void testLogin_Success() {
        // Arrange
        UsernamePasswordAuthenticationToken authenticatedToken = new UsernamePasswordAuthenticationToken(
                testUser, null, testUser.getAuthorities()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticatedToken);
        when(tokenService.generateToken(testUser))
                .thenReturn("jwt-token-example");

        // Act
        LoginResponseDTO result = authService.login(authenticationDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.token()).isEqualTo("jwt-token-example");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService).generateToken(testUser);
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

    // ==================== REGISTER TESTS ====================

    @Test
    @DisplayName("Should register new user successfully")
    void testRegister_Success() {
        // Arrange
        when(userRepository.findByLogin(registerDTO.login())).thenReturn(null);
        when(passwordEncoder.encode(registerDTO.password())).thenReturn("encrypted-password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        authService.register(registerDTO);

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();
        assertThat(savedUser.getLogin()).isEqualTo("newuser@test.com");
        assertThat(savedUser.getName()).isEqualTo("New User");
        assertThat(savedUser.getPhone()).isEqualTo("11988888888");
        assertThat(savedUser.getPassword()).isEqualTo("encrypted-password");
        assertThat(savedUser.getPassword()).isNotEqualTo("password123");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);

        verify(userRepository).findByLogin("newuser@test.com");
        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("Should throw CONFLICT when user already exists")
    void testRegister_UserAlreadyExists() {
        // Arrange
        when(userRepository.findByLogin(registerDTO.login())).thenReturn(testUser);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerDTO))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasMessage("409 CONFLICT \"User already exists\"");

        verify(userRepository).findByLogin("newuser@test.com");
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
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
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessage("404 NOT_FOUND \"User not found\"");

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
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessage("404 NOT_FOUND \"User not found\"");

        verify(userRepository).findById("non-existent");
        verify(profileRepository, never()).findByUserId(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}