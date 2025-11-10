package dk.northtech.dasscoassetservice.domain;

import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

public record DigitiserLink(
        Integer digitiser_list_id,
        String dassco_user_id,
        String username,
        String asset_guid
) {
    @JdbiConstructor
    public DigitiserLink {}
}