package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record Acknowledge(
    @Schema(description = "The guids of the assets we're gonna handle.", example = "[\"guid_1\"]")
    List<String> assetGuids,
    @Schema(description = "The status of the operation.", example = "FILE_UPLOAD_ERROR")
    AcknowledgeStatus status,
    @Schema(description = "The body of the overall status and a possible explanation on a bad status.", example = "File for asset could not be uploaded.")
    String body,
    @Schema(description = "The date the object was created.", example = "2023-05-24T00:00:00.000Z")
    Instant date) {

    @Override
    public String toString() {
        return "Acknowledge{" +
                "assetGuids=" + assetGuids +
                ", status=" + status +
                ", body='" + body + '\'' +
                ", date=" + date +
                '}';
    }
}
