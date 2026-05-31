package sodresoftwares.homebeauty.dto;

import jakarta.validation.constraints.Pattern;

public record UpdateUserFieldsDTO (
        String firstName,
        String lastName,
        String gender,

        @Pattern(regexp = "\\d{10,11}", message = "Phone number must be 10 or 11 digits")
        String phone
){}
