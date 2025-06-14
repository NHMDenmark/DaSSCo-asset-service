package dk.northtech.dasscoassetservice.assets;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("keycloak.service-user")
public record KeycloakUserConfig(String keycloakUrl, String realm, String clientId, String clientSecret, String username, String password) {
}
