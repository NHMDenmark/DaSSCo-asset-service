package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class AssetGroup {

    @Schema(description = "Asset Group Name", example = "Butterflies")
    public String group_name;
    @Schema (description = "List of assets in the group", example = "[\"Asset_1\", \"Asset_2\"]")
    public List<String> assets;
    @Schema (description = "Users with access to the Asset Group")
    public List<String> hasAccess;
}
