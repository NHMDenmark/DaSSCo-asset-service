package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record StatisticsData(
        @Schema(description = "The name of the institution which owns and digitised the specimen", example = "test-institution")
        String instituteName,
        @Schema(description = "The name of the pipeline that sent a create, update or delete request to the storage service", example = "ti-p1")
        String pipelineName,
        @Schema(description = "The name of the workstation used to do the imaging", example = "ti-ws1")
        String workstationName,
        @Schema(description = "Date and time in milliseconds", example = "19489324209000")
        Long createdDate, // event's error_timestamp in millis
        @Schema(description = "Number of specimens in the specific asset", example = "42")
        Integer specimens // number of specimens in the specific asset
) {
    public StatisticsData(String instituteName, String pipelineName, String workstationName, Long createdDate, Integer specimens) {
        this.instituteName = instituteName.replaceAll("\"", "");
        this.pipelineName = pipelineName != null ? pipelineName.replaceAll("\"", "") : null;
        this.workstationName = workstationName != null ? workstationName.replaceAll("\"", "") : null;
        this.createdDate = createdDate;
        this.specimens = specimens;
    }
}
