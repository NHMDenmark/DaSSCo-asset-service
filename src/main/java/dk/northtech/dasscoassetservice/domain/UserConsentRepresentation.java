package dk.northtech.dasscoassetservice.domain;

import java.util.List;

public record UserConsentRepresentation(String clientId, Integer createdDate, List<String> grantedClientScopes
        , Integer lastUpdatedDate) {
}
