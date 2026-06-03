package sodresoftwares.homebeauty.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record UpdateProfessionalProfileDTO(
        String description,

        @Pattern(regexp = "^$|\\d{10,11}", message = "Whatsapp must be 10 or 11 digits")
        String whatsapp,

        String instagramHandle,

        @Min(value = 1, message = "Radius must be at least 1km")
        @Max(value = 150, message = "Radius cannot exceed 150km")
        Integer serviceRadiusKm
) {}