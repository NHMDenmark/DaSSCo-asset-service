package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

public record Workstation(
        @Schema(description = "The name of the workstation", example = "ti-ws1")
        String name,
        @Schema(description = "Status of the workstation", example = "IN_SERVICE")
        WorkstationStatus status,
        @Schema(description = "The institution that where the workstation is located" , example = "test-institution")
        String institution_name) {

}
