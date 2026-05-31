package sodresoftwares.homebeauty.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import sodresoftwares.homebeauty.enums.AppointmentStatus;
import sodresoftwares.homebeauty.enums.AppointmentType;
import sodresoftwares.homebeauty.model.Appointment;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.model.user.UserRole;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("AppointmentRepository Tests")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AppointmentRepositoryTest {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    private User clientUser;
    private User professionalUser;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        // Create test users
        clientUser = User.builder()
                .firstName("Client")
                .lastName("User")
                .login("client@test.com")
                .password("password123")
                .phone("11999999999")
                .role(UserRole.USER)
                .build();

        professionalUser = User.builder()
                .firstName("Professional")
                .lastName("User")
                .login("professional@test.com")
                .password("password123")
                .phone("11988888888")
                .role(UserRole.PROFESSIONAL)
                .build();

        clientUser = userRepository.save(clientUser);
        professionalUser = userRepository.save(professionalUser);

        baseTime = LocalDateTime.of(2026, 5, 17, 10, 0, 0);
    }

    @Test
    @DisplayName("Should detect overlapping appointments")
    void testHasOverlappingAppointments() {
        // Arrange - Create an existing appointment from 10:00 to 11:00
        LocalDateTime existingStart = baseTime;
        LocalDateTime existingEnd = baseTime.plusHours(1);
        createAndSaveAppointment(clientUser, professionalUser, existingStart, existingEnd);

        // Act & Assert - Check for overlapping scenarios
        // Completely overlapping
        assertThat(appointmentRepository.hasOverlappingAppointments(
                professionalUser.getId(),
                existingStart,
                existingEnd,
                AppointmentStatus.CANCELLED
        )).isTrue();

        // Partially overlapping - starts inside existing
        assertThat(appointmentRepository.hasOverlappingAppointments(
                professionalUser.getId(),
                existingStart.plusMinutes(30),
                existingEnd.plusHours(1),
                AppointmentStatus.CANCELLED
        )).isTrue();

        // Partially overlapping - ends inside existing
        assertThat(appointmentRepository.hasOverlappingAppointments(
                professionalUser.getId(),
                existingStart.minusHours(1),
                existingStart.plusMinutes(30),
                AppointmentStatus.CANCELLED
        )).isTrue();

        // Completely contained within existing
        assertThat(appointmentRepository.hasOverlappingAppointments(
                professionalUser.getId(),
                existingStart.plusMinutes(15),
                existingEnd.minusMinutes(15),
                AppointmentStatus.CANCELLED
        )).isTrue();
    }

    @Test
    @DisplayName("Should not detect overlap when times don't overlap")
    void testHasOverlappingAppointmentsReturnsFalseWhenNoOverlap() {
        // Arrange
        LocalDateTime existingStart = baseTime;
        LocalDateTime existingEnd = baseTime.plusHours(1);
        createAndSaveAppointment(clientUser, professionalUser, existingStart, existingEnd);

        // Act & Assert - Check non-overlapping scenarios

        // Completely before
        assertThat(appointmentRepository.hasOverlappingAppointments(
                professionalUser.getId(),
                existingStart.minusHours(2),
                existingStart.minusMinutes(1),
                AppointmentStatus.CANCELLED
        )).isFalse();

        // Completely after
        assertThat(appointmentRepository.hasOverlappingAppointments(
                professionalUser.getId(),
                existingEnd.plusMinutes(1),
                existingEnd.plusHours(2),
                AppointmentStatus.CANCELLED
        )).isFalse();

        // Adjacent times (touching but not overlapping)
        assertThat(appointmentRepository.hasOverlappingAppointments(
                professionalUser.getId(),
                existingEnd,
                existingEnd.plusHours(1),
                AppointmentStatus.CANCELLED
        )).isFalse();
    }

    @Test
    @DisplayName("Should exclude cancelled appointments when checking overlap")
    void testHasOverlappingAppointmentsExcludesCancelledAppointments() {
        // Arrange
        LocalDateTime existingStart = baseTime;
        LocalDateTime existingEnd = baseTime.plusHours(1);
        
        // Create a cancelled appointment
        Appointment cancelledAppointment = createAndSaveAppointment(
                clientUser, professionalUser, existingStart, existingEnd
        );
        cancelledAppointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(cancelledAppointment);

        // Act - Check for overlap at same time as cancelled appointment
        boolean hasOverlap = appointmentRepository.hasOverlappingAppointments(
                professionalUser.getId(),
                existingStart,
                existingEnd,
                AppointmentStatus.CANCELLED
        );

        // Assert
        assertThat(hasOverlap).isFalse();
    }

    @Test
    @DisplayName("Should not find overlap for different professional")
    void testHasOverlappingAppointmentsIsProfessionalSpecific() {
        // Arrange
        LocalDateTime existingStart = baseTime;
        LocalDateTime existingEnd = baseTime.plusHours(1);
        createAndSaveAppointment(clientUser, professionalUser, existingStart, existingEnd);

        User anotherProfessional = User.builder()
                .firstName("Another")
                .lastName("Professional")
                .login("another@test.com")
                .password("password123")
                .role(UserRole.PROFESSIONAL)
                .build();
        anotherProfessional = userRepository.save(anotherProfessional);

        // Act
        boolean hasOverlap = appointmentRepository.hasOverlappingAppointments(
                anotherProfessional.getId(),
                existingStart,
                existingEnd,
                AppointmentStatus.CANCELLED
        );

        // Assert
        assertThat(hasOverlap).isFalse();
    }

    @Test
    @DisplayName("Should find active appointments for a specific day")
    void testFindActiveAppointmentsByDay() {
        // Arrange
        LocalDateTime dayStart = LocalDateTime.of(2026, 5, 18, 0, 0, 0);
        LocalDateTime dayEnd = LocalDateTime.of(2026, 5, 18, 23, 59, 59);

        LocalDateTime appointmentTime1 = LocalDateTime.of(2026, 5, 18, 10, 0, 0);
        LocalDateTime appointmentTime2 = LocalDateTime.of(2026, 5, 18, 14, 0, 0);
        LocalDateTime appointmentTime3 = LocalDateTime.of(2026, 5, 18, 18, 0, 0);

        createAndSaveAppointment(clientUser, professionalUser, appointmentTime1, appointmentTime1.plusHours(1));
        createAndSaveAppointment(clientUser, professionalUser, appointmentTime2, appointmentTime2.plusHours(1));
        createAndSaveAppointment(clientUser, professionalUser, appointmentTime3, appointmentTime3.plusHours(1));

        // Create appointment outside the day (should not appear)
        createAndSaveAppointment(clientUser, professionalUser,
                LocalDateTime.of(2026, 5, 19, 10, 0, 0),
                LocalDateTime.of(2026, 5, 19, 11, 0, 0));

        // Act
        List<Appointment> result = appointmentRepository.findActiveAppointmentsByDay(
                professionalUser.getId(),
                dayStart,
                dayEnd,
                AppointmentStatus.CANCELLED
        );

        // Assert
        assertThat(result).hasSize(3);
            assertThat(result).extracting(Appointment::getStartTime)
                    .containsExactlyInAnyOrder(appointmentTime1, appointmentTime2, appointmentTime3);
        }

    @Test
    @DisplayName("Should return empty list when no active appointments on specific day")
    void testFindActiveAppointmentsByDayReturnsEmptyWhenNoAppointments() {
        // Arrange
        LocalDateTime dayStart = LocalDateTime.of(2026, 5, 20, 0, 0, 0);
        LocalDateTime dayEnd = LocalDateTime.of(2026, 5, 20, 23, 59, 59);

        // Act
        List<Appointment> result = appointmentRepository.findActiveAppointmentsByDay(
                professionalUser.getId(),
                dayStart,
                dayEnd,
                AppointmentStatus.CANCELLED
        );

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should exclude cancelled appointments from daily results")
    void testFindActiveAppointmentsByDayExcludesCancelledAppointments() {
        // Arrange
        LocalDateTime dayStart = LocalDateTime.of(2026, 5, 21, 0, 0, 0);
        LocalDateTime dayEnd = LocalDateTime.of(2026, 5, 21, 23, 59, 59);

        LocalDateTime appointmentTime1 = LocalDateTime.of(2026, 5, 21, 10, 0, 0);
        LocalDateTime appointmentTime2 = LocalDateTime.of(2026, 5, 21, 14, 0, 0);

        Appointment activeAppointment = createAndSaveAppointment(
                clientUser, professionalUser, appointmentTime1, appointmentTime1.plusHours(1)
        );

        Appointment cancelledAppointment = createAndSaveAppointment(
                clientUser, professionalUser, appointmentTime2, appointmentTime2.plusHours(1)
        );
        cancelledAppointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(cancelledAppointment);

        // Act
        List<Appointment> result = appointmentRepository.findActiveAppointmentsByDay(
                professionalUser.getId(),
                dayStart,
                dayEnd,
                AppointmentStatus.CANCELLED
        );

        // Assert
        assertThat(result)
                .hasSize(1)
                .extracting(Appointment::getId)
                .containsExactly(activeAppointment.getId());
    }

    @Test
    @DisplayName("Should not find appointments from other professionals on the same day")
    void testFindActiveAppointmentsByDayIsProfessionalSpecific() {
        // Arrange
        LocalDateTime dayStart = LocalDateTime.of(2026, 5, 22, 0, 0, 0);
        LocalDateTime dayEnd = LocalDateTime.of(2026, 5, 22, 23, 59, 59);

        LocalDateTime appointmentTime = LocalDateTime.of(2026, 5, 22, 10, 0, 0);
        createAndSaveAppointment(clientUser, professionalUser, appointmentTime, appointmentTime.plusHours(1));

        User anotherProfessional = User.builder()
                .firstName("Another")
                .lastName("Professional")
                .login("anotherpro@test.com")
                .password("password123")
                .role(UserRole.PROFESSIONAL)
                .build();
        anotherProfessional = userRepository.save(anotherProfessional);

        // Act
        List<Appointment> result = appointmentRepository.findActiveAppointmentsByDay(
                anotherProfessional.getId(),
                dayStart,
                dayEnd,
                AppointmentStatus.CANCELLED
        );

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle appointments at day boundaries correctly")
    void testFindActiveAppointmentsByDayHandlesBoundaries() {
        // Arrange
        LocalDateTime dayStart = LocalDateTime.of(2026, 5, 23, 0, 0, 0);
        LocalDateTime dayEnd = LocalDateTime.of(2026, 5, 23, 23, 59, 59);

        // Appointment that starts at day start
        LocalDateTime appointmentAtStart = LocalDateTime.of(2026, 5, 23, 0, 0, 0);
        // Appointment that ends at day end
        LocalDateTime appointmentEndsAtEnd = LocalDateTime.of(2026, 5, 23, 22, 59, 59);

        createAndSaveAppointment(clientUser, professionalUser,
                appointmentAtStart, appointmentAtStart.plusHours(1));
        createAndSaveAppointment(clientUser, professionalUser,
                appointmentEndsAtEnd, appointmentEndsAtEnd.plusMinutes(1));

        // Act
        List<Appointment> result = appointmentRepository.findActiveAppointmentsByDay(
                professionalUser.getId(),
                dayStart,
                dayEnd,
                AppointmentStatus.CANCELLED
        );

        // Assert
        assertThat(result).hasSize(2);
    }

    // Helper method to create and save appointments
    private Appointment createAndSaveAppointment(User client, User professional, 
                                                 LocalDateTime startTime, LocalDateTime endTime) {
        Appointment appointment = Appointment.builder()
                .appointmentType(AppointmentType.PROVIDER_LOCATION)
                .startTime(startTime)
                .endTime(endTime)
                .status(AppointmentStatus.PENDING)
                .notes("Test appointment")
                .serviceName("Test Service")
                .categoryName("Test Category")
                .professionalName(professional.getFirstName() + " " + professional.getLastName())
                .price(BigDecimal.valueOf(100.00))
                .client(client)
                .professionalUser(professional)
                .build();

        return appointmentRepository.save(appointment);
    }
}

