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

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Hidden
@Component
@Path("/v1/assets/{assetGuid}/externalpublishers")
@Tag(name = "Publishers", description = "Endpoints related to publishers.")
@SecurityRequirement(name = "dassco-idp")
public class externalpublishers {

    private PublicationService publicationService;

    @Inject
    public externalpublishers(PublicationService publicationService) {
        this.publicationService = publicationService;
    }

    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Pipeline.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Publication publish(Publication publication, @PathParam("assetGuid") String assetGuid) {
        if(publication.asset_guid() != null && !publication.asset_guid().equals(assetGuid)){
            throw new IllegalArgumentException("AssetGuid in url doesnt match the one specified in the POST body");
        }
       return publicationService.publish(new Publication(publication.asset_guid(), publication.description(), publication.name()));
    }



//    @POST
//    @Path("/unpublish")
//    @Produces(MediaType.APPLICATION_JSON)
//    @Consumes(APPLICATION_JSON)
//    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
//    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Pipeline.class)))
//    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
//    public void pull(PublicationLink publicationLink, @PathParam("publisherName") String publisherName) {
//        publicationService.delete(new PublicationLink(publicationLink.asset_guid(), publicationLink.link(), publisherName, publicationLink.timestamp()));
//    }
//
//    @GET
//    @Produces(MediaType.APPLICATION_JSON)
//    @Consumes(APPLICATION_JSON)
//    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
//    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Pipeline.class)))
//    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
//    public List<PublicationLink> list(@PathParam("publisherName") String publisherName) {
//        return publicationService.list(new Publisher(publisherName));
//    }
}
