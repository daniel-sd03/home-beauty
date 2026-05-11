package sodresoftwares.homebeauty.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthenticationDTO(
        @NotBlank(message = "login is required")
        String login,

        @NotBlank(message = "Password is required")
        String password
) {}