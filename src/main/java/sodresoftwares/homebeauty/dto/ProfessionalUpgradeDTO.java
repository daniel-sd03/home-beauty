package sodresoftwares.homebeauty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProfessionalUpgradeDTO(
        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Home service info is required")
        boolean isHomeService
) {}