package dk.northtech.dasscoassetservice.domain;

import jakarta.annotation.Nullable;

public record AssetUpdateRequest(@Nullable String shareName, @Nullable MinimalAsset minimalAsset, @Nullable String workstation, @Nullable String pipeline, @Nullable String digitiser) {

}
