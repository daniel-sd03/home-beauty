package sodresoftwares.homebeauty.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import sodresoftwares.homebeauty.infra.exception.AppException;
import sodresoftwares.homebeauty.dto.CompleteUserProfileDTO;
import sodresoftwares.homebeauty.dto.ProfessionalOnboardingDTO;
import sodresoftwares.homebeauty.dto.UpdateProfessionalProfileDTO;
import sodresoftwares.homebeauty.model.ProfessionalProfile;
import sodresoftwares.homebeauty.model.Specialty;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.repositories.ProfessionalProfileRepository;
import sodresoftwares.homebeauty.repositories.SpecialtyRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfessionalProfileService - Unit Tests")
class ProfessionalProfileServiceTest {

    @Mock
    private ProfessionalProfileRepository profileRepository;

    @Mock
    private SpecialtyRepository specialtyRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ProfessionalProfileService service;

    private User mockUser;
    private Specialty mockSpecialty;
    private ProfessionalProfile existingProfile;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id("123")
                .firstName("Daniel")
                .build();

        mockSpecialty = Specialty.builder()
                .id("spec-1")
                .name("Manicure")
                .build();

        existingProfile = ProfessionalProfile.builder()
                .id("prof-999")
                .user(mockUser)
                .description("Old description")
                .serviceRadiusKm(5)
                .build();
    }

    // ==================== ONBOARDING TESTS ====================

    @Test
    @DisplayName("Should onboard professional successfully and save all fields")
    void onboardProfessionalSuccess() {
        // Arrange
        ProfessionalOnboardingDTO dto = new ProfessionalOnboardingDTO(
                "11999999999", "12345678909", LocalDate.of(1990, 1, 1), "Male",
                "Great professional", "11888888888", "@danielsodre", 10, Set.of("spec-1")
        );

        when(profileRepository.findByUserId("123")).thenReturn(Optional.empty());
        when(userService.completeUserProfile(eq("123"), any(CompleteUserProfileDTO.class))).thenReturn(mockUser);
        when(specialtyRepository.findAllById(dto.specialtyIds())).thenReturn(List.of(mockSpecialty));
        when(profileRepository.save(any(ProfessionalProfile.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        ProfessionalProfile result = service.onboardProfessional("123", dto);

        // Assert
        assertThat(result.getUser()).isEqualTo(mockUser);
        assertThat(result.getDescription()).isEqualTo("Great professional");
        assertThat(result.getWhatsapp()).isEqualTo("11888888888");
        assertThat(result.getInstagramHandle()).isEqualTo("@danielsodre");
        assertThat(result.getServiceRadiusKm()).isEqualTo(10);
        assertThat(result.getSpecialties()).hasSize(1).contains(mockSpecialty);

        verify(profileRepository, times(1)).save(any(ProfessionalProfile.class));
    }

    @Test
    @DisplayName("Should throw 409 Conflict when onboarding an already existing profile")
    void onboardProfessionalConflict() {
        ProfessionalOnboardingDTO dto = new ProfessionalOnboardingDTO(
                "11999999999", "12345678909", LocalDate.now(), "Male",
                "Desc", "11888888888", "Insta", 10, Set.of("spec-1")
        );

        when(profileRepository.findByUserId("123")).thenReturn(Optional.of(existingProfile));

        assertThatThrownBy(() -> service.onboardProfessional("123", dto))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasFieldOrPropertyWithValue("errorCode", "PROFILE_ALREADY_EXISTS")
                .hasMessage("Professional profile already exists. Use PATCH to update it.");

        verify(userService, never()).completeUserProfile(any(), any());
    }

    @Test
    @DisplayName("Should throw 400 Bad Request when one or more specialties are invalid")
    void onboardProfessionalInvalidSpecialties() {
        ProfessionalOnboardingDTO dto = new ProfessionalOnboardingDTO(
                "11999999999", "12345678909", LocalDate.now(), "Male",
                "Desc", "11888888888", "Insta", 10, Set.of("spec-1", "fake-spec") // 2 IDs
        );

        when(profileRepository.findByUserId("123")).thenReturn(Optional.empty());
        when(userService.completeUserProfile(eq("123"), any())).thenReturn(mockUser);
        when(specialtyRepository.findAllById(dto.specialtyIds())).thenReturn(List.of(mockSpecialty));

        assertThatThrownBy(() -> service.onboardProfessional("123", dto))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "INVALID_SPECIALTY_IDS")
                .hasMessage("One or more provided specialty IDs are invalid. Expected %d, but found %d.".formatted(dto.specialtyIds().size(), List.of(mockSpecialty).size()));

        verify(profileRepository, never()).save(any());
    }

    // ==================== PARTIAL UPDATE TESTS ====================

    @Test
    @DisplayName("Should update professional profile partially")
    void partialUpdateSuccess() {
        UpdateProfessionalProfileDTO dto = new UpdateProfessionalProfileDTO(
                "New description", null, "@new_insta", 20
        );

        when(profileRepository.findByUserId("123")).thenReturn(Optional.of(existingProfile));
        when(profileRepository.save(any(ProfessionalProfile.class))).thenAnswer(i -> i.getArgument(0));

        ProfessionalProfile result = service.partialUpdate("123", dto);

        // Assert updated fields
        assertThat(result.getId()).isEqualTo("prof-999");
        assertThat(result.getUser()).isEqualTo(mockUser);
        assertThat(result.getDescription()).isEqualTo("New description");
        assertThat(result.getInstagramHandle()).isEqualTo("@new_insta");
        assertThat(result.getServiceRadiusKm()).isEqualTo(20);
        assertThat(result.getWhatsapp()).isNull();

        verify(profileRepository, times(1)).save(existingProfile);
    }

    @Test
    @DisplayName("Should throw 404 Not Found when trying to update non-existent profile")
    void partialUpdateNotFound() {
        UpdateProfessionalProfileDTO dto = new UpdateProfessionalProfileDTO(
                "New description", null, null, null
        );

        when(profileRepository.findByUserId("123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.partialUpdate("123", dto))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "PROFESSIONAL_PROFILE_NOT_FOUND")
                .hasMessage("Professional profile not found");

        verify(profileRepository, never()).save(any());
    }
}