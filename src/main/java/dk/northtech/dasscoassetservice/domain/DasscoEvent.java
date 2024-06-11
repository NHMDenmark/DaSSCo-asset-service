package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "What happened to the asset", example = "UPDATE_ASSET")
public enum DasscoEvent {
    CREATE_ASSET,
    UPDATE_ASSET,
    AUDIT_ASSET,
    DELETE_ASSET,
    CREATE_ASSET_METADATA,
    UPDATE_ASSET_METADATA,
    BULK_UPDATE_ASSET_METADATA,
    AUDIT_ASSET_METADATA,
    DELETE_ASSET_METADATA,
    METADATA_TAKEN,
    ASSET_FINALISED
}
