package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

public record Digitiser (
    @Schema(description = "Dassco User Id")
    Integer dasscoUserId,
    @Schema(description = "Username of the User")
    String username
    ) {
    }

