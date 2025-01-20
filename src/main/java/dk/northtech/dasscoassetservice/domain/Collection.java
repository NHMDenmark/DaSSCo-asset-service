package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record Collection(
        @Schema(description = "The collection name within the institution that holds the specimen", example = "test-collection")
        String name,
        @Schema(description = "The name of the institution which owns and digitised the specimen", example = "test-institution")
        String institution,
        @ArraySchema(schema = @Schema( description = "If this list is not empty users need atleast one of the roles from the list in order to read/write assets from the collection",implementation = Role.class))
        List<Role> roleRestrictions) {
}
