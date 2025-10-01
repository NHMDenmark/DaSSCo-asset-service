package dk.northtech.dasscoassetservice.domain;

public record QueryProperty(
        String name,
        String dataType,
        String parent
) {
}
