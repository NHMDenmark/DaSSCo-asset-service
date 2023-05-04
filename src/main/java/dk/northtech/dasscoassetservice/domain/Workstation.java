package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

public record Workstation(
        @Schema(description = "The name of the workstation")
        String name,
        @Schema(description = "Status of the workstation")
        WorkstationStatus status,
        @Schema(description = "The institution that where the workstation is")
        String institution_name) {

}
