package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.User;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;

class KeycloakServiceTest extends AbstractIntegrationTest {
    @Inject
    KeycloakService keycloakService;

    @Test
    void syncUserFromAccessTokenCreatesUser() {
        String accessToken = createAccessToken("service-account-ars", "kc-service-account-ars");

        keycloakService.syncUserFromAccessToken(accessToken);

        Optional<User> userIfExists = userService.getUserIfExists("service-account-ars");
        assertThat(userIfExists.isPresent()).isTrue();
        assertThat(userIfExists.get().keycloak_id).isEqualTo("kc-service-account-ars");
    }

    @Test
    void syncUserFromAccessTokenUpdatesExistingUserKeycloakId() {
        userService.ensureExists(new User("service-account-update", null, null));
        String accessToken = createAccessToken("service-account-update", "kc-service-account-update");

        keycloakService.syncUserFromAccessToken(accessToken);
        userService.forceRefreshCache();

        Optional<User> userIfExists = userService.getUserIfExists("service-account-update");
        assertThat(userIfExists.isPresent()).isTrue();
        assertThat(userIfExists.get().keycloak_id).isEqualTo("kc-service-account-update");
    }

    private String createAccessToken(String preferredUsername, String sub) {
        String header = base64UrlEncode("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        String payload = base64UrlEncode("{\"preferred_username\":\"" + preferredUsername + "\",\"sub\":\"" + sub + "\"}");
        return header + "." + payload + ".signature";
    }

    private String base64UrlEncode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
