package dk.northtech.dasscoassetservice.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserRepresentation(HashMap access, HashMap attributes, UserConsentRepresentation clientConsents
        , HashMap clientRoles , Long createdTimestamp, CredentialRepresentation[] credentials
        , List<String> disableableCredentialTypes , String email, Boolean emailVerified, Boolean enabled, Boolean totp
        , List<FederatedIdentityRepresentation> federatedIdentities, String federationLink , String firstName, String groups, String id
        , String lastName, Integer notBefore, String origin , List<String> realmRoles, List<String> requiredActions
        , String self, String serviceAccountClientId , String username) {

    public UserRepresentation {
    }

    public UserRepresentation(String email) {
        this(null, null, null,null, null, null, null, email, true, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public UserRepresentation(String email, String firstName, String lastName, CredentialRepresentation[] credentialRepresentation) {
        this(null, null, null,null, null, credentialRepresentation, null, email, false, true, null, null, null, firstName, null, null, lastName, null, null, null, null, null, null, email);
    }
}
