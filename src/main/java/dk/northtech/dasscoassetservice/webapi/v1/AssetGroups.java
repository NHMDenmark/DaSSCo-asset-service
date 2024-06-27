package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.AssetGroup;
import dk.northtech.dasscoassetservice.services.AssetGroupService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.springframework.stereotype.Component;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/v1/assetgroups/")
@Tag(name = "Asset Groups", description = "Endpoints related to Asset Groups")
@SecurityRequirement(name = "dassco-idp")
public class AssetGroups {

    // TODO: Roles allowed?

    private final AssetGroupService assetGroupService;

    @Inject
    public AssetGroups(AssetGroupService assetGroupService){
        this.assetGroupService = assetGroupService;
    }

    @POST
    @Path("createassetgroup")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Create Asset Group", description = "Takes a Group Name and a List of Assets")
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public void createAssetGroup(AssetGroup assetGroup){
        this.assetGroupService.createAssetGroup(assetGroup);
    }

    @GET
    @Path("/getgroup/{groupName}")
    @Produces(APPLICATION_JSON)
    @Operation(summary = "Get Asset Group", description = "Takes a Group Name and returns the asset metadata of assets in that group")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Asset.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<Asset> getAssetGroup(@PathParam("groupName") String groupName){
        return this.assetGroupService.readAssetGroup(groupName);
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Operation(summary = "List Asset Groups", description = "Takes a Group Name and returns the asset metadata of assets in that group")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AssetGroup.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<AssetGroup> getListAssetGroup(){
        return this.assetGroupService.readListAssetGroup();
    }

    @DELETE
    @Path("/deletegroup/{groupName}")
    @Operation(summary = "Delete Asset Groups", description = "Deletes an Asset Group, takes the group name.")
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public void deleteAssetGroup(@PathParam("groupName") String groupName){
        this.assetGroupService.deleteAssetGroup(groupName);
    }

    @PUT
    @Path("/updategroup/{groupName}")
    @Operation(summary = "Update Asset Group", description = "Updates the assets in an asset group.")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Asset.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<Asset> updateAssetGroup(@PathParam("groupName") String groupName, List<String> assetList){
        return this.assetGroupService.updateAssetGroup(groupName, assetList);
    }
}
