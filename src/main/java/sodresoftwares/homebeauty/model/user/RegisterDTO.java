package sodresoftwares.homebeauty.model.user;

public record RegisterDTO(String login, String password, UserRole role, String name, String phone) {
}
