package sodresoftwares.homebeauty.dto;

import sodresoftwares.homebeauty.model.user.UserRole;

public record RegisterDTO(String login, String password, UserRole role, String name, String phone) {
}
