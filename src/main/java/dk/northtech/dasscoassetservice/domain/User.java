package dk.northtech.dasscoassetservice.domain;

import java.util.HashSet;
import java.util.Set;

public class User {
    public String username;
    public String token;
    public String keycloakId;
    public Set<String> roles = new HashSet<>();

    public User() {
    }

    public User(String username) {
        this.username = username;
    }
}
