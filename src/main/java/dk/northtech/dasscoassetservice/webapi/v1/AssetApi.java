package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.repositories.InternalStatusRepository;
import dk.northtech.dasscoassetservice.services.InternalStatusService;
import dk.northtech.dasscoassetservice.services.StatisticsDataService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Path("/v1/assets")
@SecurityRequirement(name = "dassco-idp")
public class AssetApi {
    private final StatisticsDataService specimenService;
    private final InternalStatusService internalStatusService;

    @Inject
    public AssetApi(StatisticsDataService specimenService, InternalStatusService internalStatusService) {
        this.specimenService = specimenService;
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
//        return specimenService.getSpecimenData();
        return list;
    }

    @GET
    @Path("/internalstatus")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = AssetV1.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response getInternalStatusAmt() {
        Optional<Map<InternalStatus, Integer>> statusAmts = this.internalStatusService.getInternalStatusAmt();
        return Response.status(Response.Status.OK).entity(statusAmts).build();
    }
}
