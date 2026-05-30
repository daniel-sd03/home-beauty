package sodresoftwares.homebeauty.dto;

import sodresoftwares.homebeauty.model.user.UserRole;

public record LoginResponseDTO(
        String token,
        UserRole role,
        boolean isProfileComplete
) {}