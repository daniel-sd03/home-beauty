package sodresoftwares.homebeauty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequestDTO(
        @NotBlank(message = "Street is required")
        String street,

        @NotBlank(message = "Number is required")
        String number,

        String complement,

        @NotBlank(message = "Neighborhood is required")
        String neighborhood,

        @NotBlank(message = "Zip code is required")
        @Size(min = 8, max = 8, message = "Format must be XXXXXXXX, without dashes or dots")
        String zipCode,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "State UF is required")
        @Size(min = 2, max = 2)
        String stateUf,

        @NotBlank(message = "State name is required")
        String stateName
) {
}