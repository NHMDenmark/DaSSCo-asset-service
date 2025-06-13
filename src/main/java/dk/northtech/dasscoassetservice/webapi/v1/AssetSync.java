package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.services.AssetSyncService;
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
import jakarta.ws.rs.core.Response;
import org.springframework.stereotype.Component;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/v1/amqp")
@Tag(name = "AMQP", description = "Endpoints related to the rabbitmq and Specify synchronising.")
@SecurityRequirement(name = "dassco-idp")
public class AssetSync {
    private AssetSyncService assetSyncService;

    @Inject
    public AssetSync(AssetSyncService assetSyncService) {
        this.assetSyncService = assetSyncService;
    }

//    @POST
//    @Path("/sync/all")
//    @Operation(summary = "Synchronises all completed assets to Specify.", description = "Synchronises all completed assets to Specify regardless of 'synced' property.")
//    @Produces(MediaType.APPLICATION_JSON)
//    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Specimen.class))))
//    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
//    public String synchroniseAllAssets() {
//        assetSyncService.sendAllAssetsToQueue(false);
//        return "hej";
//    }

    @POST
    @Path("/sync")
    @Operation(summary = "Synchronises all assets awaiting sync", description = "Synchronises all assets awaiting sync")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Specimen.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response synchroniseAssets() {
        assetSyncService.syncAssets();
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @POST
    @Path("/sync/{guid}")
    @Operation(summary = "Synchronise asset", description = "Synchronises asset with given guid to specify.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Specimen.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response synchroniseAsset(@PathParam("guid") String guid) {
        assetSyncService.syncAsset(guid);
        return Response.status(Response.Status.NO_CONTENT).build();
    }


}
