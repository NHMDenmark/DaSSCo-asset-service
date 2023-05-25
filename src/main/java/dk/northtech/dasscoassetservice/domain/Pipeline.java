package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

public record Pipeline(
  @Schema(description = "The name of the pipeline", example = "ti-p1") String name,
  @Schema(description = "The institution that the pipeline belongs to", example = "test-institution") String institution) {
}
