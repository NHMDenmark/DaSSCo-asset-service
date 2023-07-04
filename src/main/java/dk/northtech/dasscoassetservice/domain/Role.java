package dk.northtech.dasscoassetservice.domain;

public enum Role {
    USER(SecurityRoles.USER)
    , ADMIN(SecurityRoles.ADMIN)
    , SERVICE_USER(SecurityRoles.SERVICE)
    , DEVELOPER(SecurityRoles.DEVELOPER);

    public final String roleName;
    Role(String role) {
        this.roleName = role;
    }
}
