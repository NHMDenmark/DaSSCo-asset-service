package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashSet;
import java.util.Set;

public class User {
    @Schema(description = "Username of the user", example = "THBO")
    public String username;
    public String token;
    public String keycloakId;
    @Schema(description = "Role/s for the user", example = "ADMIN")
    public Set<String> roles = new HashSet<>();

    public User() {
    }

    public User(String username,  Set<String> roles) {
        this.username = username;
        this.roles = roles;
    }

    public User(String username) {
        this.username = username;
    }
}
