package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.AssetChange;
import dk.northtech.dasscoassetservice.domain.DasscoEvent;
import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.services.AssetChangeService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/event/change")
@Tag(name = "Event Changes", description = "Endpoints related to event changes.")
@SecurityRequirement(name = "dassco-idp")
public class AssetChangeApi {

    private final AssetChangeService assetChangeService;

    public AssetChangeApi(AssetChangeService assetChangeService) {
        this.assetChangeService = assetChangeService;
    }

    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    public Response addEventChange(AssetChange assetChange){
        var count = assetChangeService.addAssetChange(assetChange);
        return Response.status(count == 1 ? Response.Status.OK : Response.Status.BAD_REQUEST).build();
    }

    @POST
    @Path("/sync/{directoryId}/{asset_guid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    public Response synEventChange(@PathParam("directoryId") Long directoryId, @PathParam("asset_guid") String asset_guid){
        assetChangeService.syncAssetChangesToEvent(DasscoEvent.UPDATE_ASSET, directoryId, asset_guid);
        return Response.status(Response.Status.OK).build();
    }
}
