package sodresoftwares.homebeauty.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import sodresoftwares.homebeauty.enums.AppointmentStatus;
import sodresoftwares.homebeauty.model.*;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.repositories.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AvailabilityService
 *
 * This service calculates available time slots for professionals
 * considering working hours, existing appointments, and professional blocks.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AvailabilityService Tests")
class AvailabilityServiceTest {

    @Mock
    private ProfessionalProfileRepository profileRepository;

    @Mock
    private ProvidedServiceRepository serviceRepository;

    @Mock
    private WorkingHourRepository workingHoursRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ProfessionalBlockRepository blockRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    private ProfessionalProfile professionalProfile;
    private ProvidedService providedService;
    private WorkingHour workingHour;
    private User professionalUser;
    private LocalDate testDate;
    private LocalDateTime testDateTime;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.now().plusDays(1);
        testDateTime = testDate.atStartOfDay();

        professionalUser = User.builder()
                .id("professional-id")
                .firstName("John")
                .lastName("Doe")
                .build();

        professionalProfile = ProfessionalProfile.builder()
                .id("profile-id")
                .user(professionalUser)
                .build();

        providedService = ProvidedService.builder()
                .id("service-id")
                .name("Hair Cut")
                .durationMinutes(60)
                .professional(professionalProfile)
                .build();

        workingHour = WorkingHour.builder()
                .id("working-hour-id")
                .professional(professionalProfile)
                .dayOfWeek(testDate.getDayOfWeek())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .build();
    }

    @Test
    @DisplayName("Should return available slots when professional works and has no conflicts")
    void testGetAvailableSlots_Success_NoConflicts() {
        // Arrange
        when(profileRepository.findById("profile-id")).thenReturn(Optional.of(professionalProfile));
        when(serviceRepository.findById("service-id")).thenReturn(Optional.of(providedService));
        when(workingHoursRepository.findByProfessionalIdAndDayOfWeek("profile-id", testDate.getDayOfWeek()))
                .thenReturn(Optional.of(workingHour));
        when(appointmentRepository.findActiveAppointmentsByDay(
                eq("professional-id"), any(LocalDateTime.class), any(LocalDateTime.class), eq(AppointmentStatus.CANCELLED)))
                .thenReturn(List.of());
        when(blockRepository.findBlocksByDay(eq("profile-id"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // Act
        List<LocalDateTime> slots = availabilityService.getAvailableSlots("profile-id", "service-id", testDate);

        // Assert
        // 1. Verify exact size: 9:00-17:00 (8 hours = 480 min / 30 min intervals = 15 slots
        assertThat(slots).hasSize(15);

        // 2. Verify slots that MUST be available (Happy Path)
        assertThat(slots).contains(
                testDate.atTime(9, 0),   // Start of working hours
                testDate.atTime(10, 0),  // Middle of the day
                testDate.atTime(14, 0),  // Afternoon
                testDate.atTime(16, 0)  // Last slot
        );

        // 3. Verify no slots outside working hours
        assertThat(slots).doesNotContain(
                testDate.atTime(8, 30),  // Before working hours
                testDate.atTime(16, 30),  // Would end at 17:30, beyond working hours
                testDate.atTime(17, 0)   // After working hours
        );
    }

    @Test
    @DisplayName("Should return empty list when professional does not work on requested day")
    void testGetAvailableSlots_NoWorkingHours() {
        // Arrange
        when(profileRepository.findById("profile-id")).thenReturn(Optional.of(professionalProfile));
        when(serviceRepository.findById("service-id")).thenReturn(Optional.of(providedService));
        when(workingHoursRepository.findByProfessionalIdAndDayOfWeek("profile-id", testDate.getDayOfWeek()))
                .thenReturn(Optional.empty());

        // Act
        List<LocalDateTime> slots = availabilityService.getAvailableSlots("profile-id", "service-id", testDate);

        // Assert
        assertThat(slots).isEmpty();
    }

    @Test
    @DisplayName("Should exclude slots that overlap with existing appointments")
    void testGetAvailableSlots_WithAppointmentConflict() {
        // Arrange
        LocalDateTime appointmentStart = testDate.atTime(10, 0);
        LocalDateTime appointmentEnd = testDate.atTime(11, 0);

        Appointment conflictingAppointment = Appointment.builder()
                .startTime(appointmentStart)
                .endTime(appointmentEnd)
                .build();

        when(profileRepository.findById("profile-id")).thenReturn(Optional.of(professionalProfile));
        when(serviceRepository.findById("service-id")).thenReturn(Optional.of(providedService));
        when(workingHoursRepository.findByProfessionalIdAndDayOfWeek("profile-id", testDate.getDayOfWeek()))
                .thenReturn(Optional.of(workingHour));
        when(appointmentRepository.findActiveAppointmentsByDay(
                eq("professional-id"), any(LocalDateTime.class), any(LocalDateTime.class), eq(AppointmentStatus.CANCELLED)))
                .thenReturn(List.of(conflictingAppointment));
        when(blockRepository.findBlocksByDay(eq("profile-id"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // Act
        List<LocalDateTime> slots = availabilityService.getAvailableSlots("profile-id", "service-id", testDate);

        // Assert
        // 1. Verify exact size: 15 total slots - 3 conflicting slots = 12 available slots
        // Conflicting slots: 9:30 (would end at 10:30 overlapping 10:00-11:00), 10:00, 10:30
        assertThat(slots).hasSize(12);

        // 2. Verify slots that MUST be available (before and after the conflict)
        assertThat(slots).contains(
                testDate.atTime(9, 0),   // Before the appointment (9:00-10:00, no conflict)
                testDate.atTime(11, 0),  // Exactly when appointment ends (11:00-12:00, no conflict)
                testDate.atTime(14, 0),  // Well after the appointment
                testDate.atTime(16, 0)  // Last slot
        );

        // 3. Verify slots that CANNOT be available (overlap with 10:00-11:00 appointment)
        assertThat(slots).doesNotContain(
                testDate.atTime(9, 30),  // Service would run 9:30-10:30 (overlaps with appointment start)
                testDate.atTime(10, 0),  // Appointment runs exactly 10:00-11:00
                testDate.atTime(10, 30)  // Service would run 10:30-11:30 (overlaps with appointment)
        );
    }

    @Test
    @DisplayName("Should exclude slots that overlap with professional blocks")
    void testGetAvailableSlots_WithBlockConflict() {
        // Arrange
        LocalDateTime blockStart = testDate.atTime(14, 0);
        LocalDateTime blockEnd = testDate.atTime(15, 0);

        ProfessionalBlock conflictingBlock = ProfessionalBlock.builder()
                .startDateTime(blockStart)
                .endDateTime(blockEnd)
                .build();

        when(profileRepository.findById("profile-id")).thenReturn(Optional.of(professionalProfile));
        when(serviceRepository.findById("service-id")).thenReturn(Optional.of(providedService));
        when(workingHoursRepository.findByProfessionalIdAndDayOfWeek("profile-id", testDate.getDayOfWeek()))
                .thenReturn(Optional.of(workingHour));
        when(appointmentRepository.findActiveAppointmentsByDay(
                eq("professional-id"), any(LocalDateTime.class), any(LocalDateTime.class), eq(AppointmentStatus.CANCELLED)))
                .thenReturn(List.of());
        when(blockRepository.findBlocksByDay(eq("profile-id"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(conflictingBlock));

        // Act
        List<LocalDateTime> slots = availabilityService.getAvailableSlots("profile-id", "service-id", testDate);

        // Assert
        // 1. Verify exact size: 15 total slots - 3 conflicting slots = 12 available slots
        // Conflicting slots: 13:30 (would end at 14:30 overlapping 14:00-15:00), 14:00, 14:30
        assertThat(slots).hasSize(12);

        // 2. Verify slots that MUST be available (before and after the block)
        assertThat(slots).contains(
                testDate.atTime(9, 0),   // Start of day
                testDate.atTime(13, 0),  // Before the block (13:00-14:00, no conflict)
                testDate.atTime(15, 0), // After the block (15:00-16:00, no conflict)
                testDate.atTime(12, 0)   // Well before the block
        );

        // 3. Verify slots that CANNOT be available (overlap with 14:00-15:00 block)
        assertThat(slots).doesNotContain(
                testDate.atTime(13, 30), // Service would run 13:30-14:30 (overlaps with block start)
                testDate.atTime(14, 0),  // Exactly when block starts
                testDate.atTime(14, 30)  // Service would run 14:30-15:30 (overlaps with block)
        );
    }

    @Test
    @DisplayName("Should exclude past slots of the current day")
    void testGetAvailableSlots_ExcludesPastSlots() {
        // Arrange - Setup today's date in UTC
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        // We freeze the time at exactly 11:59 AM so the 12:00 PM slot is considered strictly in the future
        LocalDateTime fakeNow = today.atTime(11, 59);

        try (var mockedLocalDateTime = org.mockito.Mockito.mockStatic(LocalDateTime.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
            // Whenever the service calls LocalDateTime.now(), it will receive our fake time (11:59 AM)
            mockedLocalDateTime.when(() -> LocalDateTime.now(ZoneOffset.UTC)).thenReturn(fakeNow);

            when(profileRepository.findById("profile-id")).thenReturn(Optional.of(professionalProfile));
            when(serviceRepository.findById("service-id")).thenReturn(Optional.of(providedService));
            when(workingHoursRepository.findByProfessionalIdAndDayOfWeek("profile-id", today.getDayOfWeek()))
                    .thenReturn(Optional.of(workingHour));
            when(appointmentRepository.findActiveAppointmentsByDay(
                    eq("professional-id"), any(), any(), eq(AppointmentStatus.CANCELLED)))
                    .thenReturn(List.of());
            when(blockRepository.findBlocksByDay(eq("profile-id"), any(), any()))
                    .thenReturn(List.of());

            // Act
            List<LocalDateTime> slots = availabilityService.getAvailableSlots("profile-id", "service-id", today);

            // Assert
            // 1. Verify exact size: 15 total slots - 6 past slots (9:00, 9:30, 10:00, 10:30, 11:00, 11:30) = 9 available slots
            assertThat(slots).hasSize(9);

            // 2. Verify slots that MUST be available (Future slots from 12:00 PM onwards)
            assertThat(slots).contains(
                    today.atTime(12, 0),   // First available slot
                    today.atTime(14, 0),   // Middle of the afternoon
                    today.atTime(16, 0)    // Very last available slot
            );

            // 3. Verify slots that CANNOT be available (Past slots before 11:59 AM)
            assertThat(slots).doesNotContain(
                    today.atTime(9, 0),    // Start of the day
                    today.atTime(10, 30),  // Mid-morning
                    today.atTime(11, 30)   // The slot right before our fake current time
            );
        }
    }

    @Test
    @DisplayName("Should throw exception when requesting availability for past date")
    void testGetAvailableSlots_PastDate() {
        // Arrange
        LocalDate pastDate = LocalDate.now().minusDays(1);

        // Act & Assert
        assertThatThrownBy(() -> availabilityService.getAvailableSlots("profile-id", "service-id", pastDate))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot fetch availability for past dates")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(400);
    }

    @Test
    @DisplayName("Should throw exception when professional profile not found")
    void testGetAvailableSlots_ProfessionalNotFound() {
        // Arrange
        when(profileRepository.findById("non-existent-id")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> availabilityService.getAvailableSlots("non-existent-id", "service-id", testDate))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Professional profile not found")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);
    }

    @Test
    @DisplayName("Should throw exception when service not found")
    void testGetAvailableSlots_ServiceNotFound() {
        // Arrange
        when(profileRepository.findById("profile-id")).thenReturn(Optional.of(professionalProfile));
        when(serviceRepository.findById("non-existent-service")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> availabilityService.getAvailableSlots("profile-id", "non-existent-service", testDate))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Service not found")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);
    }

    @Test
    @DisplayName("Should handle services with different durations correctly")
    void testGetAvailableSlots_DifferentServiceDurations() {
        // Arrange - Service with 30 minutes duration
        ProvidedService shortService = ProvidedService.builder()
                .id("short-service-id")
                .name("Quick Service")
                .durationMinutes(30)
                .professional(professionalProfile)
                .build();

        when(profileRepository.findById("profile-id")).thenReturn(Optional.of(professionalProfile));
        when(serviceRepository.findById("short-service-id")).thenReturn(Optional.of(shortService));
        when(workingHoursRepository.findByProfessionalIdAndDayOfWeek("profile-id", testDate.getDayOfWeek()))
                .thenReturn(Optional.of(workingHour));
        when(appointmentRepository.findActiveAppointmentsByDay(
                eq("professional-id"), any(LocalDateTime.class), any(LocalDateTime.class), eq(AppointmentStatus.CANCELLED)))
                .thenReturn(List.of());
        when(blockRepository.findBlocksByDay(eq("profile-id"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // Act
        List<LocalDateTime> slots = availabilityService.getAvailableSlots("profile-id", "short-service-id", testDate);

        // Assert
        // 1. Verify exact size: 9:00-17:00 (8 hours = 480 min / 30 min intervals = 16 slots)
        assertThat(slots).hasSize(16);

        // 2. Verify slots that MUST be available (shorter service means more slots)
        assertThat(slots).contains(
                testDate.atTime(9, 0),   // Start of working hours
                testDate.atTime(16, 0),  // Later slot (30 min service fits here)
                testDate.atTime(16, 30)  // Very last slot (16:30-17:00)
        );

        // 3. Verify no slots outside working hours
        assertThat(slots).doesNotContain(
                testDate.atTime(8, 30),  // Before working hours
                testDate.atTime(17, 0)   // Would end at 17:30, beyond working hours
        );
    }

    @Test
    @DisplayName("Should handle multiple overlapping conflicts correctly")
    void testGetAvailableSlots_MultipleConflicts() {
        // Arrange
        LocalDateTime appointment1Start = testDate.atTime(10, 0);
        LocalDateTime appointment1End = testDate.atTime(11, 0);
        LocalDateTime blockStart = testDate.atTime(14, 0);
        LocalDateTime blockEnd = testDate.atTime(15, 0);

        Appointment appointment = Appointment.builder()
                .startTime(appointment1Start)
                .endTime(appointment1End)
                .build();

        ProfessionalBlock block = ProfessionalBlock.builder()
                .startDateTime(blockStart)
                .endDateTime(blockEnd)
                .build();

        when(profileRepository.findById("profile-id")).thenReturn(Optional.of(professionalProfile));
        when(serviceRepository.findById("service-id")).thenReturn(Optional.of(providedService));
        when(workingHoursRepository.findByProfessionalIdAndDayOfWeek("profile-id", testDate.getDayOfWeek()))
                .thenReturn(Optional.of(workingHour));
        when(appointmentRepository.findActiveAppointmentsByDay(
                eq("professional-id"), any(LocalDateTime.class), any(LocalDateTime.class), eq(AppointmentStatus.CANCELLED)))
                .thenReturn(List.of(appointment));
        when(blockRepository.findBlocksByDay(eq("profile-id"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(block));

        // Act
        List<LocalDateTime> slots = availabilityService.getAvailableSlots("profile-id", "service-id", testDate);

        // Assert
        // 1. Verify exact size: 15 total slots - 3 from appointment - 3 from block = 9 available slots
        assertThat(slots).hasSize(9);

        // 2. Verify slots that MUST be available (between/around conflicts)
        assertThat(slots).contains(
                testDate.atTime(9, 0),   // Before first appointment
                testDate.atTime(11, 0),  // Between appointment and block
                testDate.atTime(12, 0),  // Between appointment and block
                testDate.atTime(13, 0),  // Between appointment and block
                testDate.atTime(15, 0)  // After block
        );

        // 3. Verify slots that CANNOT be available (both conflicts)
        assertThat(slots).doesNotContain(
                testDate.atTime(9, 30),  // Overlaps with appointment (10:00-11:00)
                testDate.atTime(10, 0),  // Appointment slot
                testDate.atTime(10, 30), // Overlaps with appointment
                testDate.atTime(13, 30), // Overlaps with block (14:00-15:00)
                testDate.atTime(14, 0),  // Block slot
                testDate.atTime(14, 30)  // Overlaps with block
        );
    }
}
