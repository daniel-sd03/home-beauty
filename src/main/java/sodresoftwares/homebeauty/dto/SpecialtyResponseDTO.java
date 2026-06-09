package sodresoftwares.homebeauty.dto;

import sodresoftwares.homebeauty.model.Specialty;

public record SpecialtyResponseDTO(
        String id,
        String name
) {
    public SpecialtyResponseDTO(Specialty specialty) {
        this(specialty.getId(), specialty.getName());
    }
}