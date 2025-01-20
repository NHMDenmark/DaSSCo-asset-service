package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

public record Role(
        @Schema(description = "The role", example = "test-role")
        String name) {
}
