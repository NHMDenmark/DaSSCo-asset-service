package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

public record Collection(
        @Schema(description = "The name of the collection", example = "test-collection")
        String name,
        @Schema(description = "The name of the institution to which the collection belong", example = "test-institution")
        String institution) {

}
