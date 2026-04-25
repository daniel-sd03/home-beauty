package sodresoftwares.homebeauty.dto;

import jakarta.validation.constraints.NotNull;
import sodresoftwares.homebeauty.enums.AppointmentStatus;

public record AppointmentStatusUpdateDTO(
        @NotNull(message = "Appointment status is required.")
        AppointmentStatus status
) {}