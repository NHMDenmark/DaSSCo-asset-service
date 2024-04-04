package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.AssetV1;
import dk.northtech.dasscoassetservice.domain.AssetStatusInfo;
import dk.northtech.dasscoassetservice.domain.InternalStatusTimeFrame;
import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.services.InternalStatusService;
import dk.northtech.dasscoassetservice.services.StatisticsDataService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@SecurityRequirement(name = "dassco-idp")
public class AssetApi {
    private final InternalStatusService internalStatusService;
    private static final Logger logger = LoggerFactory.getLogger(AssetApi.class);

    @Inject
    public AssetApi(InternalStatusService internalStatusService) {
        this.internalStatusService = internalStatusService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = AssetV1.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<AssetV1> getAssets() {
        List<AssetV1> list = new ArrayList<>();
        list.add(new AssetV1("",Instant.now(),"","","","","",Instant.now(),"",new ArrayList<>(),new ArrayList<>(),"","",Instant.now(),new ArrayList<>(),new ArrayList<>(),new ArrayList<>(),"","", Instant.now(),"","","","","","","","","","","","","","","","","","","","","","","","","","","","","",new ArrayList<>(),"","","","","","","",new ArrayList<>(),"",""));
        return list;
    }

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
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<AssetStatusInfo> getInternalStatusAmt(@QueryParam("onlyFailed") @DefaultValue("false") boolean onlyFailed ) {
        return this.internalStatusService.getWorkInProgressAssets(onlyFailed);
    }

    @GET
    @Path("/status/{assetGuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.USER,SecurityRoles.DEVELOPER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response getAssetStatus(@PathParam("assetGuid") String assetGuid) {
        Optional<AssetStatusInfo> assetStatus = this.internalStatusService.getAssetStatus(assetGuid);
        if(assetStatus.isEmpty()) {
            return Response.status(404).build();
        }
        return Response.status(200).entity(assetStatus.get()).build();
    }
}
