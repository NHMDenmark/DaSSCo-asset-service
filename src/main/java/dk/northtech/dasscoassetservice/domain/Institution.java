package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

public record Institution(
        @Schema(description = "The name of the institution which owns and digitised the specimen", example = "test-institution")
        String name) {
}
