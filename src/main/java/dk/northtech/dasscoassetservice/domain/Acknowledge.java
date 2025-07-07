package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record Acknowledge(
        @Schema(description = "The guid of the asset that has been processed by the Specify adapter", example = "Guid-1234")
        String asset_guid,
        @Schema(description = "The status of the operation.", example = "FILE_UPLOAD_ERROR")
        AcknowledgeStatus status,
        @Schema(description = "The message of the overall status and a possible explanation on a bad status.", example = "File for asset could not be uploaded.")
        String message,
        @Schema(description = "The date the object was created.", example = "2023-05-24T00:00:00.000Z")
        Instant date,
        List<Specimen> specimensWithSpecifyIds) {


    @Override
    public String toString() {
        return "Acknowledge{" +
               "assetGuid='" + asset_guid + '\'' +
               ", status=" + status +
               ", message='" + message + '\'' +
               ", date=" + date +
               '}';
    }
}
