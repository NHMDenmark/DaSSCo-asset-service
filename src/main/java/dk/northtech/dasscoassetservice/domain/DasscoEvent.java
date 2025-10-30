package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "What happened to the asset", example = "UPDATE_ASSET")
public enum DasscoEvent {
    CREATE_ASSET,//1
    UPDATE_ASSET,//0
    AUDIT_ASSET,//1
    DELETE_ASSET,//0
    CREATE_ASSET_METADATA,//1
    UPDATE_ASSET_METADATA,//1
    BULK_UPDATE_ASSET_METADATA,//0
    AUDIT_ASSET_METADATA,//0
    DELETE_ASSET_METADATA,//0
    METADATA_TAKEN,//0
    ASSET_FINALISED,//0
    ASSET_SYNCED,//0
    SYNCHRONISE_SPECIFY,//1
}
