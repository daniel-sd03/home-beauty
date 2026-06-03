package sodresoftwares.homebeauty.dto;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CPF;
import java.time.LocalDate;
import java.util.Set;

public record ProfessionalOnboardingDTO(
        // === DADOS OBRIGATÓRIOS DO USUÁRIO ===
        @NotBlank(message = "Phone is required")
        String phone,

        @NotBlank(message = "CPF is required")
        @CPF(message = "Invalid CPF format")
        String cpf,

        @NotNull(message = "Birth date is required")
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,

        @NotBlank(message = "Gender is required")
        String gender,

        String description,

        @Pattern(regexp = "^$|\\d{10,11}", message = "Whatsapp must be 10 or 11 digits")
        String whatsapp,

        String instagramHandle,

        @NotNull(message = "Service radius is required")
        @Min(value = 1, message = "Radius must be at least 1km")
        @Max(value = 150, message = "Radius cannot exceed 150km")
        Integer serviceRadiusKm,

        @NotEmpty(message = "At least one specialty ID is required")
        Set<String> specialtyIds
) {}