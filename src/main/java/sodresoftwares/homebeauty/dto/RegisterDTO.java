package sodresoftwares.homebeauty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import sodresoftwares.homebeauty.model.user.UserRole;

public record RegisterDTO(
        @NotBlank(message = "Login is required")
        String login,

        @NotBlank(message = "Password is required")
        String password,

        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Role is required")
        UserRole role
) {}
