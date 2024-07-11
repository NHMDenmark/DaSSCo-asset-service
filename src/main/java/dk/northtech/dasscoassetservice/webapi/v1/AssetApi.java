package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.services.AssetService;
import dk.northtech.dasscoassetservice.services.InternalStatusService;
import dk.northtech.dasscoassetservice.services.RightsValidationService;
import dk.northtech.dasscoassetservice.services.StatisticsDataService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/v1/assets")
@Tag(name = "Assets", description = "Endpoints related to assets.")
@SecurityRequirement(name = "dassco-idp")
public class AssetApi {
    private final InternalStatusService internalStatusService;
    private final RightsValidationService rightsValidationService;
    private static final Logger logger = LoggerFactory.getLogger(AssetApi.class);
    private AssetService assetService;

    @Inject
    public AssetApi(InternalStatusService internalStatusService, RightsValidationService rightsValidationService, AssetService assetService) {
        this.internalStatusService = internalStatusService;
        this.assetService = assetService;
        this.rightsValidationService = rightsValidationService;
    }



    @GET
    @Operation(summary = "Get Assets", description = "Returns a list of assets.")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = AssetV1.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<AssetV1> getAssets() {
        List<AssetV1> list = new ArrayList<>();
        list.add(new AssetV1("",Instant.now(),"","","","","",Instant.now(),"",new ArrayList<>(),new ArrayList<>(),"","",Instant.now(),new ArrayList<>(),new ArrayList<>(),new ArrayList<>(),"","", Instant.now(),"","","","","","","","","","","","","","","","","","","","","","","","","","","","","",new ArrayList<>(),"","","","","","","",new ArrayList<>(),"",""));
        return list;
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

    // TODO: Hidden for now. Apparently it does not work yet, there's some change needed in the Database.
    @Hidden
    @GET
    @Path("/inprogress")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<AssetStatusInfo> getInternalStatusAmt(@QueryParam("onlyFailed") @DefaultValue("false") boolean onlyFailed ) {
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
    @Operation(summary = "Get Asset Status List", description = "Returns a list of the existing asset status in the system")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AssetStatus.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<AssetStatus> getAssetStatus(){
        return assetService.listStatus();
    }

    @GET
    @Path("/restricted_access")
    @Operation(summary = "Get Restricted Access List", description = "Returns a list of the restricted access that the Assets in the system have")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = ArrayList.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<InternalRole> getRestrictedAccess(){
        return assetService.listRestrictedAccess();
    }

}
