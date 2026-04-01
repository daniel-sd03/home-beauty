package sodresoftwares.homebeauty.model.user;

public enum UserRole {
    ADMIN("admin"),
    USER("user"),
    PROFESSIONAL("professional");

    private String role;

    UserRole(String role){
        this.role = role;
    }

    public String getRole(){
        return role;
    }
}
