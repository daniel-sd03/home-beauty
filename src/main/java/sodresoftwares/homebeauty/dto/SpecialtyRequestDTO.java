package sodresoftwares.homebeauty.dto;

import jakarta.validation.constraints.NotBlank;

public record SpecialtyRequestDTO(
        @NotBlank(message = "The specialty name cannot be empty")
        String name
) {
}