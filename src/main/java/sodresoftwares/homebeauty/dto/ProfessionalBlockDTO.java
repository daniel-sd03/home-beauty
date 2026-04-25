package sodresoftwares.homebeauty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ProfessionalBlockDTO(
        @NotBlank(message = "Title is required and cannot be empty.")
        String title,

        @NotNull(message = "Start date and time is required.")
        LocalDateTime startDateTime,

        @NotNull(message = "End date and time is required.")
        LocalDateTime endDateTime
) {}