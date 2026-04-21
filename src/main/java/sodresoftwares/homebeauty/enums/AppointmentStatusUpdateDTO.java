package sodresoftwares.homebeauty.enums;

import jakarta.validation.constraints.NotNull;

public record AppointmentStatusUpdateDTO(
        @NotNull(message = "Appointment status is required.")
        AppointmentStatus status
) {}