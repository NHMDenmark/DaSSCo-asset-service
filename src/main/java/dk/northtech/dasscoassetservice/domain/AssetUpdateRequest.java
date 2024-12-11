package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

public record AssetUpdateRequest(
        // TODO: Missing ShareName.
        @Schema(description = "Asset information consisting only on asset_guid, parent_guid, institution and collection.")
        @Nullable MinimalAsset minimalAsset,
        @Schema(description = "The name of the workstation used to do the imaging", example = "ti-ws1")
        @Nullable String workstation,
        @Schema(description = "The name of the pipeline that sent a create, update or delete request to the storage service", example = "ti-p1")
        @Nullable String pipeline,
        @Schema(description = "The name of the person who imaged the specimens (creating the assets)", example = "THBO")
        @Nullable String digitiser) {

}
