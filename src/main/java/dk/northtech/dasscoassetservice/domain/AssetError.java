package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class AssetError {
    public MinimalAsset asset;
    @Schema(description = "An internal status field used to track the status of the upload of related media", example = "COMPLETED")
    public InternalStatus status;
    @Schema(description = "An error message")
    public String errorMessage;
}
