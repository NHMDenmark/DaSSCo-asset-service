package dk.northtech.dasscoassetservice.domain;

import java.util.List;

public record QueryItem(
        String name,
        List<QueryProperty> properties
) {
}
