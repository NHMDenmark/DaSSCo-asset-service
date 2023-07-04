package dk.northtech.dasscoassetservice.webapi.domain;

import dk.northtech.dasscoassetservice.domain.MinimalAsset;
import jakarta.annotation.Nullable;

public record AssetSmbRequest(@Nullable String shareName, @Nullable MinimalAsset asset) {

}
