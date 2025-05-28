package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.services.*;
//import dk.northtech.dasscoassetservice.webapi.UserMapper;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoErrorCode;
import io.swagger.v3.oas.annotations.Hidden;
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
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/v1/assets")
@Tag(name = "Assets", description = "Endpoints related to assets.")
@SecurityRequirement(name = "dassco-idp")
public class AssetApi {
    private final InternalStatusService internalStatusService;
    private final RightsValidationService rightsValidationService;
    private final BulkUpdateService bulkUpdateService;
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(AssetApi.class);
    private AssetService assetService;

    @Inject
    public AssetApi(BulkUpdateService bulkUpdateService, InternalStatusService internalStatusService, RightsValidationService rightsValidationService, AssetService assetService, UserService userService) {
        this.internalStatusService = internalStatusService;
        this.assetService = assetService;
        this.rightsValidationService = rightsValidationService;
        this.bulkUpdateService = bulkUpdateService;
        this.userService = userService;
    }

    // TODO: Hidden for now.
    @Hidden
    @GET
    @Path("/internalstatus/{timeframe}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response getInternalStatusAmt(@PathParam("timeframe") String timeframe) {
        if (EnumUtils.isValidEnum(InternalStatusTimeFrame.class, timeframe)) {
            Map<String, Integer> statusAmts = this.internalStatusService.getCachedStatuses(InternalStatusTimeFrame.valueOf(timeframe));
            return Response.status(Response.Status.OK).entity(statusAmts).build();
        } else {
            logger.warn("Timeframe {} is not supported. Use either \"daily\" or \"total\".", timeframe);
            return Response.status(Response.Status.NO_CONTENT).entity("Timeframe {" + timeframe + "} is not supported. Use either \"daily\" or \"total\".").build();
        }
    }


    @GET
    @Path("/inprogress")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<AssetStatusInfo> getInternalStatusAmt(
            @QueryParam("onlyFailed") @DefaultValue("false") boolean onlyFailed ) {
        return this.internalStatusService.getWorkInProgressAssets(onlyFailed);
    }

    @GET
    @Path("/status/{assetGuid}")
    @Operation(summary = "Get Asset Status", description = "Returns the status of an asset.")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.USER,SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = AssetStatusInfo.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response getAssetStatus(@PathParam("assetGuid") String assetGuid) {
        Optional<AssetStatusInfo> assetStatus = this.internalStatusService.getAssetStatus(assetGuid);
        if(assetStatus.isEmpty()) {
            return Response.status(404).build();
        }
        return Response.status(200).entity(assetStatus.get()).build();
    }

    @GET
    @Path("/subjectList")
    @Operation(summary = "Get Asset Subject List", description = "Lists the existing asset subjects in the System")
    @Produces(MediaType.APPLICATION_JSON)
    // Roles allowed?
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = String.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<String> getSubjects(){
        return this.assetService.listSubjects();
    }

    @GET
    @Path(("/digitiserList"))
    @Operation(summary = "Get Digitiser List", description = "Lists the existing digitisers in the System")
    @Produces(MediaType.APPLICATION_JSON)
    // Roles allowed?
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Digitiser.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<Digitiser> getDigitisers(){
        return assetService.listDigitisers();
    }

    @GET
    @Path("/payloadTypeList")
    @Operation(summary = "Get Payload Types", description = "Returns a list of the existing payload types in the system")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = String.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<String> getPayloadTypes(){
        return assetService.listPayloadTypes();
    }

    @GET
    @Path("/statusList")
    @Operation(summary = "Get enumeration AssetStatus", description = "Get List of status that can be set in the status field in asset metadata")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AssetStatus.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<AssetStatus> getAssetStatus(){
        return Arrays.asList(AssetStatus.values());
    }

    @GET
    @Path("/restricted_access")
    @Operation(summary = "Get enum of default Dassco roles", description = "Returns list of roles, values are USER, ADMIN, SERVICE, DEVELOPER additional roles can be created by adding restrictions to institutions and collections")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = ArrayList.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<InternalRole> getRestrictedAccess(){
        return new ArrayList<>(Arrays.asList(InternalRole.values()));
    }

    @POST
    @Path("/readaccess")
    @Hidden
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get Access Permission", description = "Checks if User has access or not to certain assets.")
    public void checkAccess(@QueryParam("assetGuid") String asset_guid
            , @Context SecurityContext securityContext) {
        Optional<Asset> assetOpt = assetService.getAsset(asset_guid);
        if(assetOpt.isEmpty()) {
            throw new IllegalArgumentException("There is no such asset");
        }
        Asset asset = assetOpt.get();
        rightsValidationService.checkReadRightsThrowing(userService.from(securityContext), asset.institution, asset.collection);
    }

    @POST
    @Path("/readaccessforcsv")
    @Produces(APPLICATION_JSON)
    @Hidden
    @Operation(summary =  "Checks read access for multiple assets", description = "Checks if the User has access or not to many assets. Returns a CSV String to create the CSV file for the assets if the User has access to all the Assets or returns Forbidden + the list of assets that the User does not have permission to see.")
    public Response CsvMultipleAssets(List<String> assets, @Context SecurityContext securityContext){
        // Set: No repeated assets, just in case:
        Set<String> assetSet = new HashSet<>(assets);
        // Assets found in backend:
        List<Asset> assetList = bulkUpdateService.readMultipleAssets(assetSet.stream().toList());
        // If one or more assets don't exist, complain:
        if (assetList.size() != assetSet.size()){
            throw new IllegalArgumentException("One or more assets were not found");
        }
        // List of Assets that the User has access to:
        List<Asset> hasReadAccessTo = new ArrayList<>();

        for (Asset asset : assetList){
            boolean hasAccess = rightsValidationService.checkReadRights(userService.from(securityContext), asset.institution, asset.collection);
            if(hasAccess){
                hasReadAccessTo.add(asset);
            }
        }
        // If assets that the user has access to is different to the assets got from backend, means that User does not have access to some assets:
        if (hasReadAccessTo.size() != assetList.size()){
            // Remove assets that User HAS access to get the ones he dont:
            // Return that list to the frontend so User knows:
            Set<String> guidsToRemove = hasReadAccessTo.stream()
                    .map(Asset::getAsset_guid)
                    .collect(Collectors.toSet());
            assetSet.removeAll(guidsToRemove);

            return Response.status(403).entity(new DaSSCoError("1.0", DaSSCoErrorCode.FORBIDDEN, "User does not have access to assets: " + assetSet)).build();
        } else {
            // Return the csv String:
            return Response.status(200)
                    .entity(bulkUpdateService.createCSVString(hasReadAccessTo))
                    .build();
        }
    }

    @POST
    @Path("/readaccessforzip")
    @Hidden
    @Produces(APPLICATION_JSON)
    @Operation(summary =  "Check Read Access For Zip File Creation", description = "Checks if the User has access or not to many assets. Returns an Asset object consisting only on Institution, Collection and Asset Guid or Forbidden + the list of assets that the User does not have permission to see.")
    public Response ZipMultipleAssets(List<String> assets, @Context SecurityContext securityContext){
        // Set: No repeated assets, just in case:
        Set<String> assetSet = new HashSet<>(assets);
        // Assets found in backend:
        List<Asset> assetList = bulkUpdateService.readMultipleAssets(assetSet.stream().toList());
        // If one or more assets don't exist, complain:
        if (assetList.size() != assetSet.size()){
            throw new IllegalArgumentException("One or more assets were not found");
        }
        // List of Assets that the User has access to:
        List<Asset> hasReadAccessTo = new ArrayList<>();

        for (Asset asset : assetList){
            boolean hasAccess = rightsValidationService.checkReadRights(userService.from(securityContext), asset.institution, asset.collection);
            if(hasAccess){
                hasReadAccessTo.add(asset);
            }
        }
        // If assets that the user has access to is different to the assets got from backend, means that User does not have access to some assets:
        if (hasReadAccessTo.size() != assetList.size()){
            // Remove assets that User HAS access to get the ones he dont:
            // Return that list to the frontend so User knows:
            Set<String> guidsToRemove = hasReadAccessTo.stream()
                    .map(Asset::getAsset_guid)
                    .collect(Collectors.toSet());
            assetSet.removeAll(guidsToRemove);

            return Response.status(403).entity(new DaSSCoError("1.0", DaSSCoErrorCode.FORBIDDEN, "User does not have access to assets: " + assetSet)).build();
        } else {
            return Response.status(200)
                    .entity(hasReadAccessTo.stream().map(Asset::getAsset_guid).collect(Collectors.toList()))
                    .build();
        }
    }
}
