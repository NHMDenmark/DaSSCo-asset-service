package dk.northtech.dasscoassetservice.webapi.v1;


import dk.northtech.dasscoassetservice.domain.AssetStatusInfo;
import dk.northtech.dasscoassetservice.domain.PublicAsset;
import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.services.PublicAssetService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/extern")
@Tag(name = "PublicAssets", description = "Endpoints related to public assets.")
@SecurityRequirement(name = "dassco-idp")
public class PublicAssetApi {

    private final PublicAssetService publicAssetService;

    @Inject
    public PublicAssetApi(PublicAssetService publicAssetService) {
        this.publicAssetService = publicAssetService;
    }



    @GET
    @Path("/metadata/{assetGuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = PublicAsset.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Optional<PublicAsset> getAssetMetadata(@PathParam("assetGuid") String asset_guid) {
        return this.publicAssetService.getAsset(asset_guid);
    }
}
