package sodresoftwares.homebeauty.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyCodeDTO(
        @NotBlank(message = "login is required")
        @Email(message = "Invalid e-mail format")
        String login,

        @NotBlank(message = "Code is required")
        @Size(min = 6, max = 6, message = "Verification code must be 6 digits")
        String code
) {}