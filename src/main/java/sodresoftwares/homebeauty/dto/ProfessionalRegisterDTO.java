package sodresoftwares.homebeauty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProfessionalRegisterDTO(
        @NotBlank(message = "Login is required")
        String login,

        @NotBlank(message = "Password is required")
        String password,

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Phone is required")
        String phone,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Home service info is required")
        boolean isHomeService
) {}
