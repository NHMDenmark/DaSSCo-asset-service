package dk.northtech.dasscoassetservice.webapi.domain;

import jakarta.annotation.Nullable;

public record AssetSmbRequest(String shareName, @Nullable String assetId) {

}
