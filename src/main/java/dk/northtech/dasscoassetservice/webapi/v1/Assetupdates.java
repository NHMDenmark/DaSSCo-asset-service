package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.Audit;
import dk.northtech.dasscoassetservice.domain.Event;
import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.services.AssetService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.springframework.stereotype.Component;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/v1/assets/")
@SecurityRequirement(name = "dassco-idp")
public class Assetupdates {

    private final AssetService assetService;

    @Inject
    public Assetupdates(AssetService assetService) {
        this.assetService = assetService;
    }

    @POST
    @Path("{assetGuid}/audit")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Asset.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public boolean auditAsset(@PathParam("assetGuid") String assetGuid
    , Audit audit) {
        return this.assetService.auditAsset(audit, assetGuid);
    }
    @PUT
    @Path("{assetGuid}/unlock")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Asset.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public boolean unlockAsset(@PathParam("assetGuid") String assetGuid) {
        return this.assetService.unlockAsset(assetGuid);
    }

    @PUT
    @Path("{assetGuid}/complete")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Asset.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public boolean completeAsset(@PathParam("assetGuid") String assetGuid) {
        return this.assetService.completeAsset(assetGuid);
    }

    @GET
    @Path("{assetGuid}/events")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Asset.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<Event> getEvents(@PathParam("assetGuid") String assetGuid) {
        return this.assetService.getEvents(assetGuid);
    }

    @DELETE
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER})
//    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Collection.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public void deleteAsset() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
