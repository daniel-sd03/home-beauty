package sodresoftwares.homebeauty.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryDTO(

        String
        id,

        @NotBlank(message = "Category name is required")
        String name,

        @NotBlank(message = "Icon name is required")
        String iconName
) {}
