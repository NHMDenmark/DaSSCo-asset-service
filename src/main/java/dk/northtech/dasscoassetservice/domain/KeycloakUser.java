package dk.northtech.dasscoassetservice.domain;

public record KeycloakUser(String id, String username, String firstName, String lastName) {

    public static KeycloakUser fromUserRepresentation(UserRepresentation userRepresentation) {
        return new KeycloakUser(
                userRepresentation.id(),
                userRepresentation.username(),
                userRepresentation.firstName(),
                userRepresentation.lastName()
        );
    }
}
