package sodresoftwares.homebeauty.dto;

import jakarta.validation.constraints.NotBlank;

public record ProfessionalUpgradeDTO(
        @NotBlank(message = "Description is required")
        String description
) {}