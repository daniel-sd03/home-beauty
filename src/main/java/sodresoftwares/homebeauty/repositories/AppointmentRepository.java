package sodresoftwares.homebeauty.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sodresoftwares.homebeauty.enums.AppointmentStatus;
import sodresoftwares.homebeauty.model.Appointment;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, String> {
    List<Appointment> findByClient_IdOrderByStartTimeAsc(String clientId);
    List<Appointment> findByProfessionalUser_IdOrderByStartTimeAsc(String professionalUserId);

    // Checks if there's any active appointment that overlaps with the requested time slot
    @Query("SELECT COUNT(a) > 0 FROM Appointment a " +
            "WHERE a.professionalUser.id = :professionalId " +
            "AND a.status <> :cancelledStatus " +
            "AND a.startTime < :endTime " +
            "AND a.endTime > :startTime")
    boolean hasOverlappingAppointments(
            @Param("professionalId") String professionalId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("cancelledStatus") AppointmentStatus cancelledStatus
    );
}
