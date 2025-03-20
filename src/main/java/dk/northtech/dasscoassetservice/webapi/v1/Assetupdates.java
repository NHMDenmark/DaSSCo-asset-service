package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.services.AssetService;
import dk.northtech.dasscoassetservice.services.BulkUpdateService;
import dk.northtech.dasscoassetservice.webapi.UserMapper;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
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
import jakarta.ws.rs.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/v1/assetmetadata/")
@Tag(name = "Asset Metadata", description = "Endpoints related to asset's metadata")
@SecurityRequirement(name = "dassco-idp")
public class Assetupdates {

    private final AssetService assetService;
    private final BulkUpdateService bulkUpdateService;

    private static final Logger logger = LoggerFactory.getLogger(Assetupdates.class);

    @Inject
    public Assetupdates(AssetService assetService, BulkUpdateService bulkUpdateService) {
        this.assetService = assetService;
        this.bulkUpdateService = bulkUpdateService;
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
            , Audit audit
            , @Context SecurityContext securityContext) {
        assetService.auditAsset(UserMapper.from(securityContext),audit, assetGuid);
    }

    @PUT
    @Path("{assetGuid}/unlock")
    @Operation(summary = "Unlock Asset", description = "Unlocks an asset.")
    @Produces(MediaType.APPLICATION_JSON)
    // TODO: I changed Roles Allowed from ADMIN only to Admin + Service.
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public void unlockAsset(@PathParam("assetGuid") String assetGuid) {
        assetService.unlockAsset(assetGuid);
    }

    @POST
    @Path("{assetGuid}/assetreceived")
    @Operation(summary = "Receive Asset", description = "Changes the internal status of an asset to ASSET_RECEIVED. \n\n" +
            "Required information is: shareName and a MinimalAsset with asset_guid.")
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Hidden
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
            "The only case where this endpoint should be used is when all files belonging to an asset have been uploaded but the metadata does not have the completed status. The status should be set automatically when closing a share and syncing ERDA."
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
            "The available status are: ASSET_RECEIVED, ERDA_FAILED, ERDA_ERROR. Trying to set the status to COMPLETED will not work as there's a dedicated endpoint for that."
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
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Event.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<Event> getEvents(@PathParam("assetGuid") String assetGuid
         , @Context SecurityContext securityContext) {
        return this.assetService.getEvents(assetGuid, UserMapper.from(securityContext));
    }//check Rights

    @POST
    @Operation(summary = "Create Asset", description = "Creates asset metadata with information such as asset pid, guid, parent guid, list of specimens, funding, format of the file, workstation, pipeline, etc.\n\n" +
            "If the asset does not have a parent, the field \"parent_guid\" should be left as it is (\"string\"). If it does have a parent, the \"parent_guid\" field should have the correct information.\n\n" +
            "For the asset creation with a parent_guid to succeed, the parent has to have a file uploaded. For the creation to be successful, the minimum information to be present has to be: asset_pid, asset_guid, status, institution, collection, and digitiser. The Workstation has to be IN_SERVICE."
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime startTime = LocalDateTime.now();
        logger.info("#1: POST call to assetmetadata for asset {} with parent {} at {}",asset.asset_guid, asset.parent_guid, startTime.format(formatter));

        // Added so if the example is empty "", in the Docs the example will appear as the type "string". This converts it to null.
        if (asset.parent_guid != null && asset.parent_guid.equals("string")){
            logger.warn("Received asset with reserved parent GUID, setting it to null");
            asset.parent_guid = null;
        }
        Asset createdAsset = this.assetService.persistAsset(asset, UserMapper.from(securityContext), allocation);
        int httpCode = createdAsset.httpInfo != null ? createdAsset.httpInfo.http_allocation_status().httpCode : 500;

        LocalDateTime endTime = LocalDateTime.now();
        logger.info("API call completed at: {}. Total time: {} ms", endTime.format(formatter), java.time.Duration.between(startTime, endTime).toMillis());
        return Response.status(httpCode).entity(createdAsset).build();
    }


    @PUT
    @Path("{assetGuid}")
    @Operation(summary = "Update Asset", description = "Updates asset metadata. For an Update to be successfull it needs at least: Institution, Workstation, Pipeline, Collection, Status and updateUser. It is not possible to unlock assets via this endpoint.")
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Asset.class) ))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Asset updateAsset(@RequestBody(required = true, content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Asset.class), examples = {@ExampleObject(value = """
            {
              "asset_guid": "ti-a01-202305241657",
              "institution": "test-institution",
              "parent_guid":  null,
              "status": "BEING_PROCESSED",
              "specimens": [
                    {
                        "barcode": "bc123456",
                        "specimen_pid": "spcpid-123",
                        "preparation_type": "slide"
                    }
                ],
              "funding": "hundredetusindvis af dollars",
              "subject": "folder",
              "payload_type": "ct scan",
              "file_formats": [
                "TIF"
              ],
              "asset_locked": false,
              "restricted_access": [],
              "pipeline": "ti-p1",
              "workstation": "ti-ws1",
              "digitiser" : "thbo",
              "updateUser": "thbo",
              "tags": {
                  "testtag2": "test-tag"
              }
            }
            """)}))Asset asset
            , @PathParam("assetGuid") String assetGuid
            , @Context SecurityContext securityContext) {
        if(!Objects.equals(assetGuid, asset.asset_guid)) {
            throw new IllegalArgumentException("asset_guid in URL must match asset_guid in POST-body");
        }
        return this.assetService.updateAsset(asset, UserMapper.from(securityContext));
    }

    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Asset.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    @PUT
    @Path("/bulkUpdate")
    @Operation(summary = "Bulk Update Assets", description = """
    Update metadata in many assets at the same time. Takes a list of asse_guid and a body of properties to be updated.
    All assets in the list will have their properties overwritten by the values in the postbody. 
    """)
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    //@ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<Asset>  bulkUpdate(@RequestBody(description = "The fields to update", required = true, content = @Content(schema = @Schema(implementation = Asset.class), examples = {@ExampleObject(value = """
            {
              "status": "BEING_PROCESSED",
              "asset_locked": false,
              "subject": "Folder",
              "funding": "Hundredetusindvis af dollars",
              "payload_type": "CT scan",
              "digitiser": "Doris Digitiser",
              "pipeline": "tb-pipeline-10",
              "workstation": "tb-workstation-101",
              "updateUser": "thomas@northtech.dk"
            }
            """)})) Asset asset
            , @QueryParam("assets") List<String> assetGuids
            , @Context SecurityContext securityContext){
        // Pass an asset (the fields to be updated) and a list of assets to be updated.
        // Return type?
        // Roles Allowed?
        return this.bulkUpdateService.bulkUpdate(assetGuids, asset, UserMapper.from(securityContext));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get Asset", description = "Get the metadata on an assset")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Asset.class)))
    @ApiResponse(responseCode = "204", description = "No Content. AssetGuid does not exist.")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    @Path("/{assetGuid}")
    public Asset getAsset(@PathParam("assetGuid") String assetGuid, @Context SecurityContext securityContext) {
        return this.bulkUpdateService.checkUserRights(assetGuid, UserMapper.from(securityContext)).orElse(null);
    }

    @DELETE
    @Operation(summary = "Mark asset as deleted", description = """
    Creates a new event for the asset, with user, timestamp, pipeline, workstation and description of the event (DELETE_ASSET_METADATA). 
    Assets marked as deleted are not included in statistics but metadata and assets files are not deleted.
    """)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    @Path("/{assetGuid}")
    public void deleteAsset(@PathParam("assetGuid") String assetGuid , @Context SecurityContext securityContext) {
        this.assetService.deleteAsset(assetGuid, UserMapper.from(securityContext));
    }

    @DELETE
    @Path("/{assetGuid}/deleteMetadata")
    @Operation(summary = "Delete Asset Metadata", description = "Deletes an Assets metadata. It also removes Specimens connected only to this asset and its events.")
    @Produces(APPLICATION_JSON)
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public void deleteAssetMetadata(@PathParam("assetGuid") String assetGuid
           , @Context SecurityContext securityContext){
        this.assetService.deleteAssetMetadata(assetGuid, UserMapper.from(securityContext));
    }
}
