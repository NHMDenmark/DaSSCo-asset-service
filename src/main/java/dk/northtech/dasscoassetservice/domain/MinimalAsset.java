package dk.northtech.dasscoassetservice.domain;

import jakarta.annotation.Nullable;

public record MinimalAsset(String asset_guid, @Nullable String parent_guid) {

}
