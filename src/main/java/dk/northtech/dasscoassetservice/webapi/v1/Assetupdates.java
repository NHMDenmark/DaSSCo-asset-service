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
@Path("/v1/assetmetadata/")
@Tag(name = "Assets", description = "Endpoints related to assets.")
@SecurityRequirement(name = "dassco-idp")
public class Assetupdates {

    private final AssetService assetService;

    @Inject
    public Assetupdates(AssetService assetService) {
        this.assetService = assetService;
    }

    @POST
    @Path("{assetGuid}/audit")
    @Operation(summary = "Audit Asset", description = "Creates a new event for the asset, with user, timestamp, pipeline, workstation and description of the event (AUDIT_ASSET).\n\n" +
            "Changes \"audited\" to true.\n\n" +
            "The asset should be completed before auditing.\n\n" +
            "The asset cannot be audited by the same person that digitized it."
    )
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public void auditAsset(@PathParam("assetGuid") String assetGuid
            , Audit audit) {
        assetService.auditAsset(audit, assetGuid);
    }

    @PUT
    @Path("{assetGuid}/unlock")
    @Operation(summary = "Unlock Asset", description = "Unlocks an asset.")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN})
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public void unlockAsset(@PathParam("assetGuid") String assetGuid) {
        assetService.unlockAsset(assetGuid);
    }

    @POST
    @Path("{assetGuid}/assetreceived")
    @Operation(summary = "Receive Asset", description = "Changes the internal status of an asset to ASSET_RECEIVED")
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.USER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public void assetReceived(@Context SecurityContext securityContext, AssetUpdateRequest assetSmbRequest) {
        User user = UserMapper.from(securityContext);
        assetService.completeUpload(assetSmbRequest, user);
    }

    @POST
    @Path("{assetGuid}/complete")
    @Operation(summary = "Complete Asset", description = "Mark asset as completed.\n\n" +
            "The only case where this endpoint should be used is when all files belonging to an asset has been uploaded but the metadata does not have the completed status. The status should be set automatically when closing a share and syncing ERDA."
    )
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.USER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public void completeAsset(AssetUpdateRequest assetUpdateRequest) {
        this.assetService.completeAsset(assetUpdateRequest);
    }

    @PUT
    @Path("{assetGuid}/setstatus")
    @Operation(summary = "Set Asset Status", description = "Manually updates the status of an asset.\n\n" +
            "The available status are: METADATA_RECEIVED, ASSET_RECEIVED, COMPLETED, ERDA_FAILED, ERDA_ERROR"
    )
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public void setErrorStatus(
            @PathParam("assetGuid") String assetGuid
            , @QueryParam("newStatus") String newStatus
            , @QueryParam("errorMessage") String errorMessage) {
        assetService.setAssetStatus(assetGuid, newStatus, errorMessage);
    }

    @GET
    @Path("{assetGuid}/events")
    @Operation(summary = "Get Asset Events", description = "Shows the events associated with an asset.")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Event.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<Event> getEvents(@PathParam("assetGuid") String assetGuid) {
        return this.assetService.getEvents(assetGuid);
    }

    @POST
    @Operation(summary = "Create Asset", description = "Creates asset metadata with information such as asset pid, guid, parent guid, list of specimens, funding, format of the file, workstation, pipeline, etc.\n\n" +
            "If the asset does not have a parent, the field \"parent_guid\" should be left as it is (\"string\"). If it does have a parent, the \"parent_guid\" field should have the correct information.\n\n" +
            "For the asset creation with a parent_guid to succeed, the parent has to have a file uploaded."
    )
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
        if (asset.parent_guid != null && asset.parent_guid.equals("string")){
            asset.parent_guid = null;
        }
        Asset createdAsset = this.assetService.persistAsset(asset, UserMapper.from(securityContext), allocation);
        int httpCode = createdAsset.httpInfo != null ? createdAsset.httpInfo.http_allocation_status().httpCode : 500;
        return Response.status(httpCode).entity(createdAsset).build();
    }


    @PUT
    @Path("{assetGuid}")
    @Operation(summary = "Update Asset", description = "Updates asset metadata")
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
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get Asset", description = "Get the metadata on an assset")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Asset.class)))
    @ApiResponse(responseCode = "204", description = "No Content. AssetGuid does not exist.")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    @Path("/{assetGuid}")
    public Asset getAsset(@PathParam("assetGuid") String assetGuid) {
        // TODO: If wrong asset_guid, the code is 204. I added it as an extra code response, but maybe we could raise an exception instead.
        return this.assetService.getAsset(assetGuid).orElse(null);
    }

    @DELETE
    @Operation(summary = "Delete Asset", description = "Creates a new event for the asset, with user, timestamp, pipeline, workstation and description of the event (DELETE_ASSET_METADATA).")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    @Path("/{assetGuid}")
    public void deleteAsset(@PathParam("assetGuid") String assetGuid , @Context SecurityContext securityContext) {
        this.assetService.deleteAsset(assetGuid, UserMapper.from(securityContext));
    }
}
