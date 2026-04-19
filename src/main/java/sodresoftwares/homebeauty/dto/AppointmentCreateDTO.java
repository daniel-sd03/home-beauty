package sodresoftwares.homebeauty.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import sodresoftwares.homebeauty.enums.AppointmentType;

import java.time.LocalDateTime;

public record AppointmentCreateDTO(
        @NotBlank String providedServicesId,
        @NotNull AppointmentType appointmentType,
        @NotNull @Future LocalDateTime startTime,
        @NotNull String addressId,
        String notes
) {}