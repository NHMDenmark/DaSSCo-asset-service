package dk.northtech.dasscoassetservice.domain;

public record FederatedIdentityRepresentation(String identityProvider, String userId, String userName) {
}
