package sodresoftwares.homebeauty.dto;

import sodresoftwares.homebeauty.model.user.UserRole;

public record UserResponseDTO(
        String id,
        String name,
        String login,
        UserRole role
) {}