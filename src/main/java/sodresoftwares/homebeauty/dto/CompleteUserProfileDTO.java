package sodresoftwares.homebeauty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

public record CompleteUserProfileDTO (
        @NotBlank(message = "phone is required")
        String phone,

        @NotBlank(message = "CPF is required")
        @CPF(message = "Invalid CPF format")
        String cpf,

        @NotNull(message = "birthDate is required")
        @Past(message = "birthDate must be in the past")
        LocalDate birthDate,

        @NotBlank(message = "gender is required")
        String gender
){}


