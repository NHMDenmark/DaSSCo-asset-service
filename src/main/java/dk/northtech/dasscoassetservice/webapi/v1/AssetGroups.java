package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.services.AssetGroupService;
import dk.northtech.dasscoassetservice.services.KeycloakService;
import dk.northtech.dasscoassetservice.services.UserService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;


@Component
@Path("/v1/assetgroups/")
@Tag(name = "Asset Groups", description = "Endpoints related to Asset Groups")
@SecurityRequirement(name = "dassco-idp")
public class AssetGroups {

    private final AssetGroupService assetGroupService;
    private final UserService userService;
    private final KeycloakService keycloakService;

    @Inject
    public AssetGroups(AssetGroupService assetGroupService, UserService userService,  KeycloakService keycloakService) {
        this.assetGroupService = assetGroupService;
        this.userService = userService;
        this.keycloakService = keycloakService;
    }


    @GET
    @Path("keycloak/users")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "List Keycloak users",
            description = "Returns Keycloak users that can be granted access to asset groups."
    )
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = KeycloakUser.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<KeycloakUser> getUsers() {
        return this.keycloakService.getKeycloakUsers();
    }


    @POST
    @Path("createassetgroup")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create Asset Group", description = "Creates an asset group from a group name and a list of asset GUIDs. The caller must have read access to every asset in the group.")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AssetGroup.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Optional<AssetGroup> createAssetGroup(
            @RequestBody(
                    required = true,
                    description = "Asset group to create. Provide a unique `group_name` and the asset GUIDs to include in `assets`.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = AssetGroup.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "group_name": "Butterflies",
                                      "assets": ["dassco-asset-0001", "dassco-asset-0002"]
                                    }
                                    """)
                    )
            ) AssetGroup assetGroup,
            @Context SecurityContext securityContext) {
        return this.assetGroupService.createAssetGroup(assetGroup, userService.from(securityContext));
    }

    @GET
    @Path("/getgroup/{groupId: [0-9]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get Asset Group", description = "Takes an asset group ID and returns the asset metadata of assets in that group. User needs to have access to the Asset Group.")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Asset.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<Asset> getAssetGroup(
            @Parameter(description = "Numeric ID of the asset group to retrieve", required = true, example = "42")
            @PathParam("groupId") Integer groupId,
            @Context SecurityContext securityContext) {
        return this.assetGroupService.readAssetGroup(groupId, userService.from(securityContext));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List accessible Asset Groups", description = "Returns all existing asset groups that the caller has permission to see.")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AssetGroup.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<AssetGroup> getListAssetGroup(@Context SecurityContext securityContext) {
        return this.assetGroupService.readListAssetGroup(userService.from(securityContext));
    }

    @GET
    @Path("/owned")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List owned Asset Groups", description = "Returns asset groups created by the caller.")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AssetGroup.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<AssetGroup> getOwnListAssetGroup(@Context SecurityContext securityContext) {
        return this.assetGroupService.readOwnedAssetGroups(userService.from(securityContext));
    }

    @DELETE
    @Path("/{assetGroupId: [0-9]+}")
    @Operation(summary = "Delete Asset Group", description = "Delete a single Asset Group by ID. Only the creator (or admin) can delete a group.")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public void deleteAssetGroup(
            @Parameter(description = "Numeric ID of the asset group to delete", required = true, example = "42")
            @PathParam("assetGroupId") Integer groupId,
            @Context SecurityContext securityContext) {
        this.assetGroupService.deleteAssetGroup(groupId, userService.from(securityContext));
    }
    @POST
    @Path("/bulk-delete")
    @Operation(
            summary = "Bulk Delete Asset Groups",
            description = "Delete multiple Asset Groups by ID. Validation fails if any requested ID does not exist or the caller is not the creator of one or more groups."
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Boolean.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public boolean deleteAssetGroups(
            @RequestBody(
                    required = true,
                    description = "Numeric IDs of the asset groups to delete.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            array = @ArraySchema(schema = @Schema(implementation = Integer.class)),
                            examples = @ExampleObject(value = "[42, 43]")
                    )
            ) List<Integer> groupIds,
            @Context SecurityContext securityContext) {
        return this.assetGroupService.deleteAssetGroups(groupIds, userService.from(securityContext));
    }

    @PUT
    @Path("/updategroup/{groupId: [0-9]+}/addAssets")
    @Operation(summary = "Add Assets to Asset Group", description = "Adds asset GUIDs to an existing asset group by group ID. The caller must have permission to update the group and read access to the assets being added.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AssetGroup.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public AssetGroup addAssetsToAssetGroup(
            @Parameter(description = "Numeric ID of the asset group to update", required = true, example = "42")
            @PathParam("groupId") Integer groupId,
            @RequestBody(
                    required = true,
                    description = "Asset GUIDs to add to the asset group.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            array = @ArraySchema(schema = @Schema(implementation = String.class)),
                            examples = @ExampleObject(value = "[\"dassco-asset-0003\", \"dassco-asset-0004\"]")
                    )
            ) List<String> assetList,
            @Context SecurityContext securityContext) {
        return this.assetGroupService.addAssetsToAssetGroup(groupId, assetList, userService.from(securityContext));
    }

    @PUT
    @Path("/updategroup/{groupId: [0-9]+}/removeAssets")
    @Operation(summary = "Remove Assets from Asset Group", description = "Removes asset GUIDs from an existing asset group by group ID. The caller must have permission to update the group.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AssetGroup.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public AssetGroup removeAssetFromAssetGroup(
            @Parameter(description = "Numeric ID of the asset group to update", required = true, example = "42")
            @PathParam("groupId") Integer groupId,
            @RequestBody(
                    required = true,
                    description = "Asset GUIDs to remove from the asset group.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            array = @ArraySchema(schema = @Schema(implementation = String.class)),
                            examples = @ExampleObject(value = "[\"dassco-asset-0001\"]")
                    )
            ) List<String> assetList,
            @Context SecurityContext securityContext) {
        return this.assetGroupService.removeAssetsFromAssetGroup(groupId, assetList, userService.from(securityContext));
    }

    @PUT
    @Path("/grantAccess/{groupId: [0-9]+}")
    @Operation(summary = "Grant Access to Asset Group", description = "Grants named users access to an asset group by group ID. Users must also have permission to see the assets in the asset group.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AssetGroup.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public AssetGroup grantAccessToAssetGroup(
            @Parameter(description = "Numeric ID of the asset group to grant access to", required = true, example = "42")
            @PathParam("groupId") Integer groupId,
            @RequestBody(
                    required = true,
                    description = "Usernames to grant access to.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            array = @ArraySchema(schema = @Schema(implementation = String.class)),
                            examples = @ExampleObject(value = "[\"user@example.org\", \"curator@example.org\"]")
                    )
            ) List<String> users,
            @Context SecurityContext securityContext) {
        return this.assetGroupService.grantAccessToAssetGroup(groupId, users, userService.from(securityContext));
    }
    @PUT
    @Path("keycloak/grantAccess/{groupId: [0-9]+}")
    @Operation(summary = "Grant Keycloak Users Access to Asset Group", description = "Grants Keycloak users access to an asset group by group ID. Users must also have permission to see the assets in the asset group.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AssetGroup.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public AssetGroup grantKeycloakUserAccessToAssetGroup(
            @Parameter(description = "Numeric ID of the asset group to grant access to", required = true, example = "42")
            @PathParam("groupId") Integer groupId,
            @RequestBody(
                    required = true,
                    description = "Keycloak users to grant access to.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            array = @ArraySchema(schema = @Schema(implementation = KeycloakUser.class)),
                            examples = @ExampleObject(value = """
                                    [
                                      {
                                        "id": "9f3a2d8a-5b4c-4a0a-9f61-2f4e8b3f7d12",
                                        "username": "curator@example.org",
                                        "firstName": "Casey",
                                        "lastName": "Curator"
                                      }
                                    ]
                                    """)
                    )
            ) List<KeycloakUser> keycloakUsers,
            @Context SecurityContext securityContext) {
        return this.assetGroupService.grantKeycloakUserAccessToAssetGroup(groupId, keycloakUsers, userService.from(securityContext));
    }

    @PUT
    @Path("/revokeAccess/{groupId: [0-9]+}")
    @Operation(summary = "Revoke Access to Asset Group", description = "Revokes named users' access to an asset group by group ID. The caller must have permission to update access for the group.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AssetGroup.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public AssetGroup revokeAccessToAssetGroup(
            @Parameter(description = "Numeric ID of the asset group to revoke access from", required = true, example = "42")
            @PathParam("groupId") Integer groupId,
            @RequestBody(
                    required = true,
                    description = "Usernames to revoke access from.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            array = @ArraySchema(schema = @Schema(implementation = String.class)),
                            examples = @ExampleObject(value = "[\"user@example.org\"]")
                    )
            ) List<String> users,
            @Context SecurityContext securityContext) {
        return this.assetGroupService.revokeAccessToAssetGroup(groupId, users, userService.from(securityContext));
    }
}
