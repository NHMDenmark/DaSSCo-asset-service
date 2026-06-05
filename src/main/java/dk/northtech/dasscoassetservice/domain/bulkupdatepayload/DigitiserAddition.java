package dk.northtech.dasscoassetservice.domain.bulkupdatepayload;

import dk.northtech.dasscoassetservice.domain.KeycloakUser;

import java.util.List;

public record DigitiserAddition(
        Integer dasscoUserId,
        KeycloakUser keycloakUser,
        List<String> assetGuids
) {}
