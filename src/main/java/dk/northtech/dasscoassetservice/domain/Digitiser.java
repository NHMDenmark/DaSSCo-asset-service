package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

public record Digitiser (
    @Schema(description = "Unique Identifier")
    String userId,
    @Schema(description = "Username of the User")
    String name){}

