package sodresoftwares.homebeauty.dto;

import sodresoftwares.homebeauty.enums.AppointmentStatus;
import sodresoftwares.homebeauty.enums.AppointmentType;
import sodresoftwares.homebeauty.model.Appointment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AppointmentResponseDTO(
        String id,
        String serviceName,
        String categoryName,
        String professionalName,
        BigDecimal price,
        LocalDateTime startTime,
        LocalDateTime endTime,
        AppointmentStatus status,
        AppointmentType appointmentType,
        String notes
) {
    public AppointmentResponseDTO(Appointment entity) {
        this(
                entity.getId(),
                entity.getServiceName(),
                entity.getCategoryName(),
                entity.getProfessionalName(),
                entity.getPrice(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getStatus(),
                entity.getAppointmentType(),
                entity.getNotes()
        );
    }
}