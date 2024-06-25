package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record Collection(
        @Schema(description = "The collection name within the institution that holds the specimen", example = "test-collection")
        String name,
        @Schema(description = "The name of the institution which owns and digitised the specimen", example = "test-institution")
        String institution,
        @Schema(description = "If this list contains roles then users attempting to read assets in this collection will need one of the listed roles")
        List<Role> roleRestrictions) {

}
