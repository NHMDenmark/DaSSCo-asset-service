package dk.northtech.dasscoassetservice.domain;

public record CredentialRepresentation (Integer createdDate, String credentialData, String id, Integer priority
        , String secretData, Boolean temporary, String type, String userLabel, String value) {

    public CredentialRepresentation {
    }

    public CredentialRepresentation(String value) {
        this(null, null, null, null, null, false, null, null, value);
    }
}
