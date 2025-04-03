package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest extends AbstractIntegrationTest{
    @Test
    void ensureExists() {
        User user = new User("Urzula", "kc-urzula", null);
        User result = userService.ensureExists(user);
        assertThat(result.dassco_user_id).isNotNull();
        assertThat(result.dassco_user_id).isGreaterThan(0);
    }

    @Test
    void ensureExistsUpdating() {
        User user = new User("ensureExistsUpdating", null, null);
        User result = userService.ensureExists(user);
        assertThat(result.dassco_user_id).isNotNull();
        assertThat(result.dassco_user_id).isGreaterThan(0);
        user.keycloak_id = "Kay Cee Eye Dee";
        userService.ensureExists(user);
        userService.forceRefreshCache();
        Optional<User> userIfExists = userService.getUserIfExists(user.username);
        assertThat(userIfExists.isPresent()).isTrue();
        assertThat(userIfExists.get().keycloak_id).isEqualTo("Kay Cee Eye Dee");
    }
}