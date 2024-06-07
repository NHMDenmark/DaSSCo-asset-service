package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

public record Pipeline(
  @Schema(description = "The name of the pipeline that sent a create, update or delete request to the storage service", example = "ti-p1")
  String name,
  @Schema(description = "The name of the institution which owns and digitised the specimen", example = "test-institution")
  String institution) {
}
