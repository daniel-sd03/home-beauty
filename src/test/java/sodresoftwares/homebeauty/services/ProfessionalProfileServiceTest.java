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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import sodresoftwares.homebeauty.dto.ProfessionalRegisterDTO;
import sodresoftwares.homebeauty.dto.ProfessionalUpgradeDTO;
import sodresoftwares.homebeauty.model.ProfessionalProfile;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.model.user.UserRole;
import sodresoftwares.homebeauty.repositories.ProfessionalProfileRepository;
import sodresoftwares.homebeauty.repositories.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfessionalProfileService Tests")
class ProfessionalProfileServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProfessionalProfileRepository profileRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProfessionalProfileService professionalProfileService;

    private User testUser;
    private ProfessionalRegisterDTO registerDTO;
    private ProfessionalUpgradeDTO upgradeDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-123")
                .login("original@test.com")
                .name("João Silva")
                .phone("11999998888")
                .password("hash-seguro")
                .role(UserRole.USER)
                .build();

        registerDTO = new ProfessionalRegisterDTO(
                "prof@test.com", "password123", "Jane", "119...", "Expert Hair"
        );
        upgradeDTO = new ProfessionalUpgradeDTO("Expert Consultant");

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    // ==================== REGISTER NEW PROFESSIONAL ====================

    @Test
    @DisplayName("Should register new professional and create profile successfully")
    void shouldRegisterNewProfessionalSuccessfully() {
        // Arrange
        when(userRepository.findByLogin(registerDTO.login())).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pass");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        professionalProfileService.registerNewProfessional(registerDTO);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getRole()).isEqualTo(UserRole.PROFESSIONAL);
        assertThat(savedUser.getPassword()).isEqualTo("hashed-pass");
        assertThat(savedUser.getPassword()).isNotEqualTo("password123");

        // Assert
        ArgumentCaptor<ProfessionalProfile> profileCaptor = ArgumentCaptor.forClass(ProfessionalProfile.class);
        verify(profileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getDescription()).isEqualTo("Expert Hair");
        assertThat(profileCaptor.getValue().getUser()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("Should throw CONFLICT when registering professional with existing login")
    void shouldThrowConflictWhenUserExists() {
        when(userRepository.findByLogin(anyString())).thenReturn(testUser);

        assertThatThrownBy(() -> professionalProfileService.registerNewProfessional(registerDTO))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        verify(profileRepository, never()).save(any());
    }

    // ==================== UPGRADE TO PROFESSIONAL ====================

    @Test
    @DisplayName("Should upgrade existing user and create professional profile")
    void shouldUpgradeToProfessionalSuccessfully() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(profileRepository.findByUserId(testUser.getId())).thenReturn(Optional.empty());

        // Act
        professionalProfileService.upgradeToProfessional(upgradeDTO);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getId()).isEqualTo("user-123");
        assertThat(savedUser.getLogin()).isEqualTo("original@test.com");
        assertThat(savedUser.getName()).isEqualTo("João Silva");
        assertThat(savedUser.getPhone()).isEqualTo("11999998888");
        assertThat(savedUser.getPassword()).isEqualTo("hash-seguro");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.PROFESSIONAL);

        // Assert
        ArgumentCaptor<ProfessionalProfile> profileCaptor = ArgumentCaptor.forClass(ProfessionalProfile.class);
        verify(profileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getDescription()).isEqualTo("Expert Consultant");
        assertThat(profileCaptor.getValue().getUser()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("Should throw CONFLICT when user already has a professional profile")
    void shouldThrowConflictWhenProfileAlreadyExists() {
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(profileRepository.findByUserId(anyString())).thenReturn(Optional.of(new ProfessionalProfile()));

        assertThatThrownBy(() -> professionalProfileService.upgradeToProfessional(upgradeDTO))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        verify(userRepository, never()).save(any());
    }
}