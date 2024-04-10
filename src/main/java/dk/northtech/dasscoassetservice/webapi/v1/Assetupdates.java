package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.services.AssetService;
import dk.northtech.dasscoassetservice.webapi.UserMapper;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.Operation;
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
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.springframework.stereotype.Component;

import javax.print.attribute.standard.Media;
import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Tag(name = "Assets", description = "Endpoints related to assets.")
@Path("/v1/assetmetadata/")
@SecurityRequirement(name = "dassco-idp")
public class Assetupdates {

    private final AssetService assetService;

    @Inject
    public Assetupdates(AssetService assetService) {
        this.assetService = assetService;
    }

    @POST
    @Operation(summary = "Audit Asset", description = "Creates a new event for the asset, with user, timestamp, pipeline, workstation and description of the event (AUDIT_ASSET).\n\n" +
                                                        "Changes \"audited\" to true."
    )
    // TODO: returns boolean, should I change the docs? application_text
    @Path("{assetGuid}/audit")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Asset.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public boolean auditAsset(@PathParam("assetGuid") String assetGuid
            , Audit audit) {
        return this.assetService.auditAsset(audit, assetGuid);
    }

    @PUT
    @Operation(summary = "Unlock Asset", description = "Unlocks an asset.")
    @Path("{assetGuid}/unlock")
    // TODO: Returns a boolean. reutrn an object that has success = true not text or boolean
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Asset.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public boolean unlockAsset(@PathParam("assetGuid") String assetGuid) {
        return this.assetService.unlockAsset(assetGuid);
    }

    @POST
    @Operation(summary = "Receive Asset", description = "Changes the internal status of an asset to ASSET_RECEIVED")
    // TODO: Instead of returning a body returns a boolean. Should I change that?
    @Path("{assetGuid}/assetreceived")
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.USER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Asset.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public boolean assetreceived(@Context SecurityContext securityContext, AssetUpdateRequest assetSmbRequest) {
        User user = UserMapper.from(securityContext);
        return this.assetService.completeUpload(assetSmbRequest, user);
    }

    @POST
    @Operation(summary = "Complete Asset", description = "Mark asset as completed.\n\n" +
                                                            "The only case where this endpoint should be used is when all files belonging to an asset has been uploaded but the metadata dont have the completed status. The status should be set automatically when closing a share and syncing ERDA."
    )
    @Path("{assetGuid}/complete")
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.USER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    // TODO: Documentation says it should be a PUT method. It also returns Code 204 (but it works). Does not have a Response Body for Success.
    public void completeAsset(AssetUpdateRequest assetUpdateRequest) {
        this.assetService.completeAsset(assetUpdateRequest);
    }

    @PUT
    @Operation(summary = "Set Asset Status", description = "Manually updates the status of an asset.\n\n" +
                                                            "The available status are: METADATA_RECEIVED, ASSET_RECEIVED, COMPLETED, ERDA_FAILED, ERDA_ERROR")
    @Path("{assetGuid}/setstatus")
    // TODO: Returns true or false, should I change it?
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Asset.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public boolean setErrorStatus(
            @PathParam("assetGuid") String assetGuid
            , @QueryParam("newStatus") String newStatus
            , @QueryParam("errorMessage") String errorMessage) {
        return this.assetService.setAssetStatus(assetGuid, newStatus, errorMessage);
    }

    @GET
    // TODO: It returns the events. Should I change the docs?
    @Path("{assetGuid}/events")
    @Operation(summary = "Get Asset Events", description = "Shows the events associated with an asset.")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Asset.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<Event> getEvents(@PathParam("assetGuid") String assetGuid) {
        return this.assetService.getEvents(assetGuid);
    }

    @POST
    // TODO: The endpoint does not return sambaInfo
    @Operation(summary = "Create Asset", description = "Creates asset metadata. This initialises the file upload by opening a SMB share where files belonging to the asset can be uploaded. The endpoint returns the created asset metadata with an additional field called sambaInfo that provides the connection parameters for the share.\n\n" +
                                                        "If the service fails to create the share the metadata is still persisted and a share can be opened manually using the open share endpoint.")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Asset.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response createAsset(
            Asset asset
            , @Context SecurityContext securityContext
            , @QueryParam("allocation_mb") int allocation
            ) {
        // Added so if the example is empty "", in the Docs the example will appear as the type "string". This converts it to null.
        if (asset != null){
            if (asset.parent_guid.equals("string")){
                asset.parent_guid = null;
            }
        }
        Asset createdAsset = this.assetService.persistAsset(asset, UserMapper.from(securityContext), allocation);
        int httpCode = createdAsset.httpInfo != null ? createdAsset.httpInfo.http_allocation_status().httpCode : 500;
        return Response.status(httpCode).entity(createdAsset).build();
    }


    @PUT
    @Operation(summary = "Update Asset", description = "Updates asset metadata")
    @Path("{assetGuid}")
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Asset.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Asset updateAsset(Asset asset,
                             @PathParam("assetGuid") String assetGuid) {
        asset.asset_guid = assetGuid;
        return this.assetService.updateAsset(asset);
    }

    @GET
    @Operation(summary = "Get Asset", description = "Get the metadata on an assset")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Asset.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    @Path("/{assetGuid}")
    public Asset getAsset(@PathParam("assetGuid") String assetGuid) {
        return this.assetService.getAsset(assetGuid).orElse(null);
    }

    @DELETE
    // TODO: Returns a 204 also. Change.
    @Operation(summary = "Delete Asset", description = "Creates a new event for the asset, with user, timestamp, pipeline, workstation and description of the event (DELETE_ASSET_METADATA).")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Asset.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    @Path("/{assetGuid}")
    public void deleteAsset(@PathParam("assetGuid") String assetGuid , @Context SecurityContext securityContext) {
        this.assetService.deleteAsset(assetGuid, UserMapper.from(securityContext));
    }
}
