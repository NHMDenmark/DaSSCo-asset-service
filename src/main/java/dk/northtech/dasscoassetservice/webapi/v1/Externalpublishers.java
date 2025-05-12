package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.Pipeline;
import dk.northtech.dasscoassetservice.domain.Publication;
import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.services.PublicationService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.springframework.stereotype.Component;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Hidden
@Component
@Path("/v1/assetmetadata/{assetGuid}/externalpublishers/")
@Tag(name = "Publishers", description = "Endpoints related to publishers.")
@SecurityRequirement(name = "dassco-idp")
public class Externalpublishers {

    private PublicationService publicationService;

    @Inject
    public Externalpublishers(PublicationService publicationService) {
        this.publicationService = publicationService;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Publication.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Publication publish(Publication publication, @PathParam("assetGuid") String assetGuid) {
        if(publication.asset_guid() != null && !publication.asset_guid().equals(assetGuid)){
            throw new IllegalArgumentException("AssetGuid in url doesnt match the one specified in the POST body");
        }
       return publicationService.publish(new Publication(assetGuid, publication.description(), publication.name()));
    }

    @PUT
    @Path("/{publicationId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Publication.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Publication update(Publication publication
            , @PathParam("assetGuid") String assetGuid
            , @PathParam("publicationId") Long publicationId) {
        if(publication.asset_guid() != null && !publication.asset_guid().equals(assetGuid)){
            throw new IllegalArgumentException("AssetGuid in url doesnt match the one specified in the POST body");
        }
        if(publication.publication_id() != null && !publication.publication_id().equals(publicationId)) {
            throw new IllegalArgumentException("Ids in url and post body doesnt match");
        }
        return publicationService.update(new Publication(publicationId, assetGuid, publication.description(), publication.name()));
    }



    @DELETE
    @Path("/{publicationId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Publication.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public void delete(@PathParam("publicationId") int publicationId) {
        publicationService.delete(publicationId);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Publication.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<Publication> list(@PathParam("assetGuid") String assetGuid) {
        return publicationService.list(assetGuid);
    }
}
