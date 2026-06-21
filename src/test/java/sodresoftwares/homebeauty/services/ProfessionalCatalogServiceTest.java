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
import sodresoftwares.homebeauty.dto.ProfessionalBlockDTO;
import sodresoftwares.homebeauty.dto.ProvidedServiceDTO;
import sodresoftwares.homebeauty.dto.WorkingHourDTO;
import sodresoftwares.homebeauty.enums.AppointmentStatus;
import sodresoftwares.homebeauty.enums.ServiceLocationType;
import sodresoftwares.homebeauty.infra.exception.AppException;
import sodresoftwares.homebeauty.model.*;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.repositories.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProfessionalCatalogService
 *
 * Tests business logic for professional services, working hours, and blocks.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfessionalCatalogService Tests")
class ProfessionalCatalogServiceTest {

    @Mock
    private ProfessionalProfileRepository profileRepository;

    @Mock
    private ProvidedServiceRepository providedServiceRepository;

    @Mock
    private WorkingHourRepository workingHourRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProfessionalBlockRepository blockRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private ProfessionalCatalogService catalogService;

    private User currentUser;
    private ProfessionalProfile professionalProfile;
    private Category testCategory;
    private ProvidedService testService;
    private WorkingHour testWorkingHour;
    private ProfessionalBlock testBlock;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id("user-123")
                .login("professional@test.com")
                .build();

        professionalProfile = ProfessionalProfile.builder()
                .id("prof-123")
                .user(currentUser)
                .description("Experienced beautician")
                .workingHours(new java.util.ArrayList<>())
                .services(new java.util.ArrayList<>())
                .blocks(new java.util.ArrayList<>())
                .build();

        testCategory = Category.builder()
                .id("cat-hair")
                .name("Hair")
                .iconName("scissors")
                .build();

        testService = ProvidedService.builder()
                .id("serv-123")
                .name("Hair Cut")
                .description("Professional hair cut")
                .locationType(ServiceLocationType.CLIENT_LOCATION_ONLY)
                .price(BigDecimal.valueOf(50.0))
                .durationMinutes(30)
                .professional(professionalProfile)
                .category(testCategory)
                .build();

        testWorkingHour = WorkingHour.builder()
                .id("wh-123")
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .professional(professionalProfile)
                .build();

        testBlock = ProfessionalBlock.builder()
                .id("block-123")
                .title("Vacation")
                .startDateTime(LocalDateTime.now().plusDays(1))
                .endDateTime(LocalDateTime.now().plusDays(8))
                .professional(professionalProfile)
                .build();

    }

    // ==================== PROVIDED SERVICE TESTS ====================

    @Test
    @DisplayName("Should add provided service successfully")
    void shouldAddProvidedServiceSuccessfully() {
        // Arrange
        ProvidedServiceDTO serviceDTO = new ProvidedServiceDTO(
                null,
                "Hair Cut",
                "Professional hair cut",
                ServiceLocationType.CLIENT_LOCATION_ONLY,
                BigDecimal.valueOf(50.0),
                30,
                "cat-hair",
                List.of("https://example.com/image1.jpg")
        );

        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));
        when(categoryRepository.findById("cat-hair")).thenReturn(Optional.of(testCategory));
        when(providedServiceRepository.save(any(ProvidedService.class))).thenReturn(null);

        // Act
        catalogService.addProvidedService(currentUser, serviceDTO);

        // Assert
        ArgumentCaptor<ProvidedService> serviceCaptor = ArgumentCaptor.forClass(ProvidedService.class);
        verify(providedServiceRepository).save(serviceCaptor.capture());

        ProvidedService savedService = serviceCaptor.getValue();
        assertThat(savedService.getName()).isEqualTo("Hair Cut");
        assertThat(savedService.getDescription()).isEqualTo("Professional hair cut");
        assertThat(savedService.getPrice()).isEqualTo(BigDecimal.valueOf(50.0));
        assertThat(savedService.getDurationMinutes()).isEqualTo(30);
        assertThat(savedService.getProfessional().getId()).isEqualTo("prof-123");
        assertThat(savedService.getCategory().getId()).isEqualTo("cat-hair");
    }

    @Test
    @DisplayName("Should throw FORBIDDEN when adding service without professional profile")
    void shouldThrowForbiddenWhenNoProfessionalProfile() {
        // Arrange
        ProvidedServiceDTO serviceDTO = new ProvidedServiceDTO(
                null, "Hair Cut", "Description", ServiceLocationType.CLIENT_LOCATION_ONLY, BigDecimal.valueOf(50.0), 30, "cat-hair", null
        );

        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> catalogService.addProvidedService(currentUser, serviceDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("errorCode", "PROFESSIONAL_PROFILE_NOT_FOUND")
                .hasMessage("Access denied: User does not have a professional profile");

        verify(providedServiceRepository, never()).save(any(ProvidedService.class));
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when category does not exist")
    void shouldThrowNotFoundWhenCategoryNotExist() {
        // Arrange
        ProvidedServiceDTO serviceDTO = new ProvidedServiceDTO(
                null, "Hair Cut", "Description", ServiceLocationType.CLIENT_LOCATION_ONLY, BigDecimal.valueOf(50.0), 30, "non-existent", null
        );

        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));
        when(categoryRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> catalogService.addProvidedService(currentUser, serviceDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "CATEGORY_NOT_FOUND")
                .hasMessage("Category not found with the provided ID");

        verify(providedServiceRepository, never()).save(any(ProvidedService.class));
    }

    @Test
    @DisplayName("Should get my provided services successfully")
    void shouldGetMyProvidedServicesSuccessfully() {
        // Arrange
        professionalProfile.getServices().add(testService);
        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));

        // Act
        List<ProvidedServiceDTO> result = catalogService.getMyProvidedServices(currentUser);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Hair Cut");
        assertThat(result.get(0).price()).isEqualTo(BigDecimal.valueOf(50.0));
        assertThat(result.get(0).durationMinutes()).isEqualTo(30);
    }

    @Test
    @DisplayName("Should update service successfully")
    void shouldUpdateServiceSuccessfully() {
        // Arrange
        ProvidedServiceDTO updateDTO = new ProvidedServiceDTO(
                "serv-123",
                "Premium Hair Cut",
                "Updated description",
                ServiceLocationType.PROVIDER_LOCATION_ONLY,
                BigDecimal.valueOf(75.0),
                45,
                "cat-hair",
                null
        );

        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));
        when(providedServiceRepository.findById("serv-123")).thenReturn(Optional.of(testService));
        when(categoryRepository.findById("cat-hair")).thenReturn(Optional.of(testCategory));
        when(providedServiceRepository.save(any(ProvidedService.class))).thenReturn(null);

        // Act
        catalogService.updateService(currentUser, "serv-123", updateDTO);

        // Assert
        ArgumentCaptor<ProvidedService> serviceCaptor = ArgumentCaptor.forClass(ProvidedService.class);
        verify(providedServiceRepository).save(serviceCaptor.capture());

        ProvidedService updatedService = serviceCaptor.getValue();
        assertThat(updatedService.getName()).isEqualTo("Premium Hair Cut");
        assertThat(updatedService.getDescription()).isEqualTo("Updated description");
        assertThat(updatedService.getPrice()).isEqualTo(BigDecimal.valueOf(75.0));
        assertThat(updatedService.getDurationMinutes()).isEqualTo(45);
        assertThat(updatedService.getCategory().getId()).isEqualTo("cat-hair");
        assertThat(updatedService.getImages()).isEmpty();

    }

    @Test
    @DisplayName("Should throw FORBIDDEN when updating service of another professional")
    void shouldThrowForbiddenWhenUpdatingOtherProfessionalService() {
        // Arrange
        User otherUser = User.builder().id("other-user").build();
        ProfessionalProfile otherProfile = ProfessionalProfile.builder()
                .id("other-prof")
                .user(otherUser)
                .build();
        testService.setProfessional(otherProfile);

        ProvidedServiceDTO updateDTO = new ProvidedServiceDTO(
                "serv-123", "New Name", "Desc", ServiceLocationType.CLIENT_LOCATION_ONLY, BigDecimal.valueOf(50.0), 30, "cat-hair", null
        );

        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));
        when(providedServiceRepository.findById("serv-123")).thenReturn(Optional.of(testService));

        // Act & Assert
        assertThatThrownBy(() -> catalogService.updateService(currentUser, "serv-123", updateDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("errorCode", "SERVICE_ACCESS_DENIED")
                .hasMessage("You do not have permission to edit this service");

        verify(providedServiceRepository, never()).save(any(ProvidedService.class));
    }

    @Test
    @DisplayName("Should delete service successfully")
    void shouldDeleteServiceSuccessfully() {
        // Arrange
        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));
        when(providedServiceRepository.findById("serv-123")).thenReturn(Optional.of(testService));
        doNothing().when(providedServiceRepository).delete(testService);

        // Act
        catalogService.deleteService(currentUser, "serv-123");

        // Assert
        verify(providedServiceRepository).delete(testService);
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when deleting non-existent service")
    void shouldThrowNotFoundWhenDeletingNonExistentService() {
        // Arrange
        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));
        when(providedServiceRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> catalogService.deleteService(currentUser, "non-existent"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "SERVICE_NOT_FOUND")
                .hasMessage("Service not found");

        verify(providedServiceRepository, never()).delete(any(ProvidedService.class));
    }

    @Test
    @DisplayName("Should throw FORBIDDEN when deleting service of another professional")
    void shouldThrowForbiddenWhenDeletingOtherProfessionalService() {
        // Arrange
        User otherUser = User.builder().id("other-user").build();
        ProfessionalProfile otherProfile = ProfessionalProfile.builder()
                .id("other-prof")
                .user(otherUser)
                .build();
        testService.setProfessional(otherProfile);

        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));
        when(providedServiceRepository.findById("serv-123")).thenReturn(Optional.of(testService));

        // Act & Assert
        assertThatThrownBy(() -> catalogService.deleteService(currentUser, "serv-123"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("errorCode", "SERVICE_ACCESS_DENIED")
                .hasMessage("You do not have permission to delete this service");

        verify(providedServiceRepository, never()).delete(any(ProvidedService.class));
    }

    // ==================== WORKING HOUR TESTS ====================

    @Test
    @DisplayName("Should add working hour successfully")
    void shouldAddWorkingHourSuccessfully() {
        // Arrange
        WorkingHourDTO whDTO = new WorkingHourDTO(
                null,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));
        when(workingHourRepository.save(any(WorkingHour.class))).thenReturn(null);

        // Act
        catalogService.addWorkingHour(currentUser, whDTO);

        // Assert
        ArgumentCaptor<WorkingHour> whCaptor = ArgumentCaptor.forClass(WorkingHour.class);
        verify(workingHourRepository).save(whCaptor.capture());

        WorkingHour savedWH = whCaptor.getValue();
        assertThat(savedWH.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(savedWH.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(savedWH.getEndTime()).isEqualTo(LocalTime.of(17, 0));
        assertThat(savedWH.getProfessional().getId()).isEqualTo("prof-123");
    }

    @Test
    @DisplayName("Should get my working hours successfully")
    void shouldGetMyWorkingHoursSuccessfully() {
        // Arrange
        professionalProfile.getWorkingHours().add(testWorkingHour);
        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));

        // Act
        List<WorkingHourDTO> result = catalogService.getMyWorkingHours(currentUser);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(result.get(0).startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.get(0).endTime()).isEqualTo(LocalTime.of(17, 0));
    }

    @Test
    @DisplayName("Should update working hour successfully")
    void shouldUpdateWorkingHourSuccessfully() {
        // Arrange
        WorkingHourDTO updateDTO = new WorkingHourDTO(
                "wh-123",
                DayOfWeek.TUESDAY,
                LocalTime.of(8, 0),
                LocalTime.of(18, 0)
        );

        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));
        when(workingHourRepository.findById("wh-123")).thenReturn(Optional.of(testWorkingHour));
        when(workingHourRepository.save(any(WorkingHour.class))).thenReturn(null);

        // Act
        catalogService.updateWorkingHour(currentUser, "wh-123", updateDTO);

        // Assert
        ArgumentCaptor<WorkingHour> whCaptor = ArgumentCaptor.forClass(WorkingHour.class);
        verify(workingHourRepository).save(whCaptor.capture());

        WorkingHour updatedWH = whCaptor.getValue();
        assertThat(updatedWH.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(updatedWH.getStartTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(updatedWH.getEndTime()).isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when updating non-existent working hour")
    void shouldThrowNotFoundWhenUpdatingNonExistentWorkingHour() {
        // Arrange
        WorkingHourDTO updateDTO = new WorkingHourDTO(
                "non-existent", DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(18, 0)
        );
        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));
        when(workingHourRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> catalogService.updateWorkingHour(currentUser, "non-existent", updateDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "WORKING_HOUR_NOT_FOUND")
                .hasMessage("Working hour not found");

        verify(workingHourRepository, never()).save(any(WorkingHour.class));
    }

    @Test
    @DisplayName("Should throw FORBIDDEN when updating working hour of another professional")
    void shouldThrowForbiddenWhenUpdatingOtherProfessionalWorkingHour() {
        // Arrange
        User otherUser = User.builder().id("other-user").build();
        ProfessionalProfile otherProfile = ProfessionalProfile.builder()
                .id("other-prof")
                .user(otherUser)
                .build();
        testWorkingHour.setProfessional(otherProfile);

        WorkingHourDTO updateDTO = new WorkingHourDTO(
                "wh-123", DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(18, 0)
        );

        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));
        when(workingHourRepository.findById("wh-123")).thenReturn(Optional.of(testWorkingHour));

        // Act & Assert
        assertThatThrownBy(() -> catalogService.updateWorkingHour(currentUser, "wh-123", updateDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("errorCode", "WORKING_HOUR_ACCESS_DENIED")
                .hasMessage("You do not have permission to edit this working hour");

        verify(workingHourRepository, never()).save(any(WorkingHour.class));
    }

    @Test
    @DisplayName("Should delete working hour successfully")
    void shouldDeleteWorkingHourSuccessfully() {
        // Arrange
        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));
        when(workingHourRepository.findById("wh-123")).thenReturn(Optional.of(testWorkingHour));
        doNothing().when(workingHourRepository).delete(testWorkingHour);

        // Act
        catalogService.deleteWorkingHour(currentUser, "wh-123");

        // Assert
        verify(workingHourRepository).delete(testWorkingHour);
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when deleting non-existent working hour")
    void shouldThrowNotFoundWhenDeletingNonExistentWorkingHour() {
        // Arrange
        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));
        when(workingHourRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> catalogService.deleteWorkingHour(currentUser, "non-existent"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "WORKING_HOUR_NOT_FOUND")
                .hasMessage("Working hour not found");

        verify(workingHourRepository, never()).delete(any(WorkingHour.class));
    }

    @Test
    @DisplayName("Should throw FORBIDDEN when deleting working hour of another professional")
    void shouldThrowForbiddenWhenDeletingOtherProfessionalWorkingHour() {
        // Arrange
        User otherUser = User.builder().id("other-user").build();
        ProfessionalProfile otherProfile = ProfessionalProfile.builder()
                .id("other-prof")
                .user(otherUser)
                .build();
        testWorkingHour.setProfessional(otherProfile);

        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));
        when(workingHourRepository.findById("wh-123")).thenReturn(Optional.of(testWorkingHour));

        // Act & Assert
        assertThatThrownBy(() -> catalogService.deleteWorkingHour(currentUser, "wh-123"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("errorCode", "WORKING_HOUR_ACCESS_DENIED")
                .hasMessage("You do not have permission to delete this working hour");

        verify(workingHourRepository, never()).delete(any(WorkingHour.class));
    }

    // ==================== BLOCK TESTS ====================

    @Test
    @DisplayName("Should create block successfully")
    void shouldCreateBlockSuccessfully() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);;
        LocalDateTime endDate = startDate.plusDays(5).withSecond(0).withNano(0);;

        ProfessionalBlockDTO blockDTO = new ProfessionalBlockDTO(
                "Vacation",
                startDate,
                endDate
        );

        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));
        when(appointmentRepository.hasOverlappingAppointments(
                "user-123",
                startDate.withSecond(0).withNano(0),
                endDate.withSecond(0).withNano(0),
                AppointmentStatus.CANCELLED
        )).thenReturn(false);
        when(blockRepository.hasOverlappingBlocks(
                "prof-123",
                startDate.withSecond(0).withNano(0),
                endDate.withSecond(0).withNano(0)
        )).thenReturn(false);
        when(blockRepository.save(any(ProfessionalBlock.class))).thenReturn(null);

        // Act
        catalogService.createBlock(currentUser, blockDTO);

        // Assert
        ArgumentCaptor<ProfessionalBlock> blockCaptor = ArgumentCaptor.forClass(ProfessionalBlock.class);
        verify(blockRepository).save(blockCaptor.capture());

        ProfessionalBlock savedBlock = blockCaptor.getValue();
        assertThat(savedBlock.getTitle()).isEqualTo("Vacation");
        assertThat(savedBlock.getProfessional().getId()).isEqualTo("prof-123");
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when creating block in the past")
    void shouldThrowBadRequestWhenBlockInPast() {
        // Arrange
        LocalDateTime pastStart = LocalDateTime.now().minusDays(1).withSecond(0).withNano(0);;
        LocalDateTime pastEnd = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);;

        ProfessionalBlockDTO blockDTO = new ProfessionalBlockDTO("Past Block", pastStart, pastEnd);

        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));

        // Act & Assert
        assertThatThrownBy(() -> catalogService.createBlock(currentUser, blockDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "BLOCK_PAST_DATE")
                .hasMessage("Cannot create blocks in the past (UTC 0 reference).");

        verify(blockRepository, never()).save(any(ProfessionalBlock.class));
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when block exceeds 30 days")
    void shouldThrowBadRequestWhenBlockExceeds30Days() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);;
        LocalDateTime endDate = startDate.plusDays(31).withSecond(0).withNano(0);;

        ProfessionalBlockDTO blockDTO = new ProfessionalBlockDTO("Long Block", startDate, endDate);

        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));

        // Act & Assert
        assertThatThrownBy(() -> catalogService.createBlock(currentUser, blockDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "BLOCK_TOO_LONG")
                .hasMessage("A single block cannot exceed 30 days.");

        verify(blockRepository, never()).save(any(ProfessionalBlock.class));
    }

    @Test
    @DisplayName("Should throw CONFLICT when block overlaps with appointment")
    void shouldThrowConflictWhenBlockOverlapsAppointment() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);;
        LocalDateTime endDate = startDate.plusDays(5).withSecond(0).withNano(0);;

        ProfessionalBlockDTO blockDTO = new ProfessionalBlockDTO("Vacation", startDate, endDate);

        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));
        when(appointmentRepository.hasOverlappingAppointments(
                "user-123",
                startDate.withSecond(0).withNano(0),
                endDate.withSecond(0).withNano(0),
                AppointmentStatus.CANCELLED
        )).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> catalogService.createBlock(currentUser, blockDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasFieldOrPropertyWithValue("errorCode", "BLOCK_APPOINTMENT_CONFLICT")
                .hasMessage("It is not possible to block this period because there are already scheduled.");

        verify(blockRepository, never()).save(any(ProfessionalBlock.class));
    }

    @Test
    @DisplayName("Should throw CONFLICT when block overlaps with existing block")
    void shouldThrowConflictWhenBlockOverlapsExistingBlock() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);;
        LocalDateTime endDate = startDate.plusDays(5).withSecond(0).withNano(0);;

        ProfessionalBlockDTO blockDTO = new ProfessionalBlockDTO("Vacation", startDate, endDate);

        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));
        when(appointmentRepository.hasOverlappingAppointments(
                "user-123",
                startDate.withSecond(0).withNano(0),
                endDate.withSecond(0).withNano(0),
                AppointmentStatus.CANCELLED
        )).thenReturn(false);
        when(blockRepository.hasOverlappingBlocks(
                "prof-123",
                startDate.withSecond(0).withNano(0),
                endDate.withSecond(0).withNano(0)
        )).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> catalogService.createBlock(currentUser, blockDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasFieldOrPropertyWithValue("errorCode", "BLOCK_OVERLAP_CONFLICT")
                .hasMessage("Cannot create block: this period overlaps with an already existing block.");

        verify(blockRepository, never()).save(any(ProfessionalBlock.class));
    }

    @Test
    @DisplayName("Should get my blocks successfully")
    void shouldGetMyBlocksSuccessfully() {
        // Arrange
        when(profileRepository.findByUserId("user-123")).thenReturn(Optional.of(professionalProfile));
        when(blockRepository.findByProfessionalIdOrderByStartDateTimeAsc("prof-123"))
                .thenReturn(List.of(testBlock));

        // Act
        var result = catalogService.getMyBlocks(currentUser);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Vacation");
        verify(blockRepository).findByProfessionalIdOrderByStartDateTimeAsc("prof-123");
    }
}