package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The current status of an asset", example = "BEING_PROCESSED")
public enum AssetStatus {
    WORKING_COPY
    , ARCHIVE
    , BEING_PROCESSED
    , PROCESSING_HALTED
    , ISSUE_WITH_MEDIA
    , ISSUE_WITH_METADATA
    , FOR_DELETION
    ;
}
