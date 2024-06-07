package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

public record Publisher(
        @Schema(description = "Name of the publisher")
        String name) {

}
