package dk.northtech.dasscoassetservice.services;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import dk.northtech.dasscoassetservice.assets.KeycloakUserConfig;
import dk.northtech.dasscoassetservice.domain.KeycloakUser;
import dk.northtech.dasscoassetservice.domain.UserRepresentation;
import dk.northtech.dasscoassetservice.utils.CustomKeycloakTokenDeserializer;
import dk.northtech.dasscoassetservice.utils.KeycloakAuthenticator;
import dk.northtech.dasscoassetservice.webapi.domain.KeycloakToken;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class KeycloakService {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakService.class);
    KeycloakAuthenticator keycloakAuthenticator;
    KeycloakUserConfig keycloakUserConfig;
    ObjectMapper objectMapper = new ObjectMapper();
    SimpleModule module = new SimpleModule("CustomKeycloakTokenDeserializer", new Version(1, 0, 0, null, null, null));
    private static KeycloakToken keycloakToken;
    private static HttpClient httpClient;

    @Inject
    public KeycloakService(KeycloakAuthenticator keycloakAuthenticator, KeycloakUserConfig keycloakUserConfig) {
        this.keycloakAuthenticator = keycloakAuthenticator;
        this.keycloakUserConfig = keycloakUserConfig;
        this.module.addDeserializer(KeycloakToken.class, new CustomKeycloakTokenDeserializer());
        this.objectMapper.registerModule(module);
    }

    public String getUserServiceToken() {
        // Given we have an access token
        if (keycloakToken != null) {
            // Validate the expiration
            // If it's almost ran out, try to refresh
            if (keycloakToken.accessExpirationTimeStamp().isBefore(Instant.now().plusSeconds(30))) {
                LOGGER.debug("KeycloakService: Attempt refresh!");
                // If the refresh token is still valid, use refresh token
                if (keycloakToken.refreshExpirationTimeStamp().isBefore(Instant.now().plusSeconds(30))) {
                    LOGGER.debug("KeycloakService: Refreshing!");
                    return newAccessToken().accessToken();
                }
                // If it's not valid, then fall through and create a new token

                // else: Just reuse the old access token.
            } else {
                LOGGER.debug("KeycloakService: Using old AccessToken for: " + (keycloakToken.accessExpirationTimeStamp().getEpochSecond() - Instant.now().plusSeconds(30).getEpochSecond()) + " seconds");
                return keycloakToken.accessToken();
            }
        }
        LOGGER.debug("KeycloakService: Create new AccessToken");
        return newAccessToken().accessToken();
    }



    public KeycloakToken refreshToken() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(this.keycloakUserConfig.keycloakUrl() + "realms/" + this.keycloakUserConfig.realm() + "/protocol/openid-connect/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=refresh_token&" +
                            "refresh_token=" + keycloakToken.refreshToken() + "&" +
                            "scope=openid offline_access&" +
                            "client_id=" + this.keycloakUserConfig.clientId()))
                    .build();

            HttpResponse<String> response = HttpClient.newBuilder()
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            String json = response.body();

            keycloakToken = objectMapper.readValue(json, KeycloakToken.class);
            return keycloakToken;
        } catch (URISyntaxException | InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public KeycloakToken newAccessToken() {
        //Order new token token
        try {
            String clientCredentials = this.keycloakUserConfig.clientId() + ":" + this.keycloakUserConfig.clientSecret();
            String base64ClientCredentials = Base64.getEncoder().encodeToString(clientCredentials.getBytes());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(this.keycloakUserConfig.keycloakUrl() + "realms/" + this.keycloakUserConfig.realm() + "/protocol/openid-connect/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    //.header("Authorization", "Basic " + base64ClientCredentials)
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials&"
                            + "client_id=" + this.keycloakUserConfig.clientId() + "&" + "client_secret=" + this.keycloakUserConfig.clientSecret()
                            + "&scope=openid offline_access"))
                    .build();

            HttpResponse<String> response = HttpClient.newBuilder()
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            String json = response.body();

            keycloakToken = objectMapper.readValue(json, KeycloakToken.class);
            return keycloakToken;
        } catch (URISyntaxException | InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    public KeycloakToken getQueueToken(){
        return newAccessToken();
    }


    public List<UserRepresentation> getUsers(String search) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(this.keycloakUserConfig.keycloakUrl() + "admin/realms/" + this.keycloakUserConfig.realm() + "/users" + (!search.isEmpty() ? "?search=" + search : "")))
                    .header("Authorization", "Bearer " + this.getUserServiceToken())
                    .GET()
                    .build();

            try(HttpClient httpClient = HttpClient.newBuilder().build()) {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                String json = response.body();
                LOGGER.debug("KeycloakService getUsers response status: {}, body: {}", response.statusCode(), json);
                
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Failed to get users from Keycloak. Status: " + response.statusCode() + ", Response: " + json);
                }
                
                // Check if response is an array (starts with '[') or an error object (starts with '{')
                if (json != null && json.trim().startsWith("{")) {
                    throw new RuntimeException("Keycloak returned an error response: " + json);
                }
                
                return objectMapper.readValue(json, new TypeReference<List<UserRepresentation>>() {});
            }
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public List<KeycloakUser> getKeycloakUsers(String search) {
        return getUsers(search).stream()
                .map(KeycloakUser::fromUserRepresentation)
                .toList();
    }
}
