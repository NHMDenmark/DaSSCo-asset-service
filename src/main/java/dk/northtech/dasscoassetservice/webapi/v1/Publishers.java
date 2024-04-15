package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.Pipeline;
import dk.northtech.dasscoassetservice.domain.PublicationLink;
import dk.northtech.dasscoassetservice.domain.Publisher;
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
@Path("/v1/publishers/{publisherName}")
@Tag(name = "Publishers", description = "Endpoints related to publishers.")
@SecurityRequirement(name = "dassco-idp")
public class Publishers {

    private PublicationService publicationService;

    @Inject
    public Publishers(PublicationService publicationService) {
        this.publicationService = publicationService;
    }

    @POST
    @Path("/publish")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Pipeline.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public PublicationLink publish(PublicationLink publicationLink, @PathParam("publisherName") String publisherName) {
       return publicationService.publish(new PublicationLink(publicationLink.asset_guid(), publicationLink.link(), publisherName, publicationLink.timestamp()));
    }

    @POST
    @Path("/pull")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Pipeline.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public void pull(PublicationLink publicationLink, @PathParam("publisherName") String publisherName) {
        publicationService.pull(new PublicationLink(publicationLink.asset_guid(), publicationLink.link(), publisherName, publicationLink.timestamp()));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Pipeline.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<PublicationLink> list(@PathParam("publisherName") String publisherName) {
        return publicationService.list(new Publisher(publisherName));
    }
}
