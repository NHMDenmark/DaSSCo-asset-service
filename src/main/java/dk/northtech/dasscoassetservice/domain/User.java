package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class User {
    @Schema(description = "Username of the user", example = "THBO")
    public String username;
    public transient String token;
    public String keycloak_id;
    @Schema(description = "Role/s for the user", example = "ADMIN")
    public Set<String> roles = new HashSet<>();
    public Integer dassco_user_id;

    public User() {
    }

    @JdbiConstructor
    public User(String username, String keycloak_id, Integer dassco_user_id) {
        this.username = username;
        this.keycloak_id = keycloak_id;
        this.dassco_user_id = dassco_user_id;
    }

    public User(String username, Set<String> roles) {
        this.username = username;
        this.roles = roles;
    }

    public User(String username) {
        this.username = username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username) && Objects.equals(roles, user.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, roles);
    }
}
