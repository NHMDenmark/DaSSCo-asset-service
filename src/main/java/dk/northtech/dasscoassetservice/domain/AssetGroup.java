package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class AssetGroup {

    @Schema(description = "Asset Group ID", example = "42")
    public Integer group_id;

    @Schema(description = "Asset Group Name", example = "Butterflies")
    public String group_name;
    @Schema (description = "List of assets in the group", example = "[\"Asset_1\", \"Asset_2\"]")
    public List<String> assets;
    @Schema (description = "Users with access to the Asset Group")
    public List<String> hasAccess;
    @Schema (description = "Keycloak users with access to the Asset Group")
    public List<KeycloakUser> keycloakUsers;
    @Schema (description = "User who created the Asset Group originially")
    public String groupCreator;
}
