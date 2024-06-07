package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

public record Collection(
        @Schema(description = "The collection name within the institution that holds the specimen", example = "test-collection")
        String name,
        @Schema(description = "The name of the institution which owns and digitised the specimen", example = "test-institution")
        String institution) {

}
