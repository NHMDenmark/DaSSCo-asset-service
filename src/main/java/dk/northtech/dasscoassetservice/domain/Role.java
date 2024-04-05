package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Possible roles for the User", example = "ADMIN")
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
