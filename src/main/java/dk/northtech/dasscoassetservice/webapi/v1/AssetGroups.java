package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.AssetGroup;
import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.services.AssetGroupService;
import dk.northtech.dasscoassetservice.webapi.UserMapper;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/v1/assetgroups/")
@Tag(name = "Asset Groups", description = "Endpoints related to Asset Groups")
@SecurityRequirement(name = "dassco-idp")
public class AssetGroups {

   private final AssetGroupService assetGroupService;

    @Inject
    public AssetGroups(AssetGroupService assetGroupService){
        this.assetGroupService = assetGroupService;
    }

    @POST
    @Path("createassetgroup")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Create Asset Group", description = "Takes a Group Name and a List of Assets. User needs at least read role on the assets to create an asset_group")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Optional<AssetGroup> createAssetGroup(AssetGroup assetGroup, @Context SecurityContext securityContext){
        return this.assetGroupService.createAssetGroup(assetGroup, UserMapper.from(securityContext));
    }

    @GET
    @Path("/getgroup/{groupName}")
    @Produces(APPLICATION_JSON)
    @Operation(summary = "Get Asset Group", description = "Takes a Group Name and returns the asset metadata of assets in that group. User needs to have access to the Asset Group.")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Asset.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<Asset> getAssetGroup(@PathParam("groupName") String groupName, @Context SecurityContext securityContext){
        return this.assetGroupService.readAssetGroup(groupName, UserMapper.from(securityContext));
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Operation(summary = "List Asset Groups", description = "Returns a list of the existing Asset Groups, only those the user has permission to see.")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AssetGroup.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<AssetGroup> getListAssetGroup(@Context SecurityContext securityContext){
        return this.assetGroupService.readListAssetGroup(UserMapper.from(securityContext));
    }

    @GET
    @Path("/owned")
    @Produces(APPLICATION_JSON)
    @Operation(summary = "List Asset Groups", description = "Returns a list of the existing Asset Groups, only those the user has permission to see.")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AssetGroup.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<AssetGroup> getOwnListAssetGroup(@Context SecurityContext securityContext){
        return this.assetGroupService.readOwnedAssetGroups(UserMapper.from(securityContext));
    }

    @DELETE
    @Path("/deletegroup/{groupName}")
    @Operation(summary = "Delete Asset Groups", description = "Deletes an Asset Group, using the Asset Group name. Only the user that created the group can delete it.")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public void deleteAssetGroup(@PathParam("groupName") String groupName, @Context SecurityContext securityContext){
        this.assetGroupService.deleteAssetGroup(groupName, UserMapper.from(securityContext));
    }

    @PUT
    @Path("/updategroup/{groupName}/addAssets")
    @Operation(summary = "Add Assets to Asset Group", description = "Adds assets to the asset group.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AssetGroup.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public AssetGroup addAssetsToAssetGroup(@PathParam("groupName") String groupName, List<String> assetList, @Context SecurityContext securityContext){
        return this.assetGroupService.addAssetsToAssetGroup(groupName, assetList, UserMapper.from(securityContext));
    }

    @PUT
    @Path("/updategroup/{groupName}/removeAssets")
    @Operation(summary = "Remove Assets from Asset Group", description = "Removes assets from the asset group.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AssetGroup.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public AssetGroup removeAssetFromAssetGroup(@PathParam("groupName") String groupName, List<String> assetList, @Context SecurityContext securityContext){
        return this.assetGroupService.removeAssetsFromAssetGroup(groupName, assetList, UserMapper.from(securityContext));
    }

    @PUT
    @Path("/grantAccess/{groupName}")
    @Operation(summary = "Grant Access to Asset Group", description = "Gives access to other users to the Asset Group. Users have to have permission to see the assets in the Asset Group")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AssetGroup.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public AssetGroup grantAccessToAssetGroup(@PathParam("groupName") String groupName, List<String> users, @Context SecurityContext securityContext){
     return this.assetGroupService.grantAccessToAssetGroup(groupName, users, UserMapper.from(securityContext));
    }

    @PUT
    @Path("/revokeAccess/{groupName}")
    @Operation(summary = "Revoke Access to Asset Group", description = "Revokes access to other users to the Asset Group.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AssetGroup.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public AssetGroup revokeAccessToAssetGroup(@PathParam("groupName") String groupName, List<String> users, @Context SecurityContext securityContext){
     return this.assetGroupService.revokeAccessToAssetGroup(groupName, users, UserMapper.from(securityContext));
    }
}
