package sodresoftwares.homebeauty.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProvidedServiceDTO(
        String
        id,

        @NotBlank(message = "ProvidedService name is required")
        String name,

        String description,

        @NotNull(message = "Price is required")
        @PositiveOrZero(message = "Price must be zero or greater")
        BigDecimal price,

        @NotNull(message = "Duration is required")
        @Min(value = 1, message = "Duration must be at least 1 minutes")
        Integer durationMinutes,

        @NotBlank(message = "Category ID is required")
        String categoryId
) {}
