package uz.itpu.teamwork.project.auth.enums;

public enum UserRole {
    ADMIN("ROLE_ADMIN", "Administrator with full system access"),
    MANAGER("ROLE_MANAGER", "Manager with product and order management access"),
    CUSTOMER("ROLE_CUSTOMER", "Customer with basic access");

    private final String authority;
    private final String description;

    UserRole(String authority, String description) {
        this.authority = authority;
        this.description = description;
    }

    public String getAuthority() {
        return authority;
    }

    public String getDescription() {
        return description;
    }
}