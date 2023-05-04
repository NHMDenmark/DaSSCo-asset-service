package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

public record Institution(@Schema(description = "The institution name", example = "NNAD") String name) {
}
