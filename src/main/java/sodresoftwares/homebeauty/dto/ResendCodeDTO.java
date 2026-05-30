package sodresoftwares.homebeauty.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendCodeDTO(
        @NotBlank(message = "login is required")
        @Email(message = "Invalid e-mail format")
        String login
) {}
