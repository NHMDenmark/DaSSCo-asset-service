package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A status field used to track the status of Acknowledge object we receive in the queue from the Specify Adapter", example = "SUCCESS")
public enum AcknowledgeStatus {
    SUCCESS,
    FILE_UPLOAD_ERROR,
    FILE_DOWNLOAD_ERROR,
    SPECIMENT_NOT_FOUND_ERROR,
    METADATA_UPLOAD_ERROR,
    UNKOWN_ERROR,
    NO_MAPPINGS_FOUND
}
