package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.services.CollectionService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.springframework.stereotype.Component;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
@ApiOperation("Collection API")
@Component
@Path("/v1/institutions/{institutionName}/collections/")
@SecurityRequirement(name = "dassco-idp")
public class Collections {
    private CollectionService collectionService;

    @Inject
    public Collections(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiOperation(value = "List collections",  notes = "Lists all collections that belongs to given institution")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Collection.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<Collection> getInstitutes(@PathParam("institutionName") String institutionName
            , @Context SecurityContext securityContext) {
        return this.collectionService.listCollections(new Institution(institutionName));
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Collection.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Collection createInstitution(
            @PathParam("institutionName") String institutionName
            , Collection collection) {
        return this.collectionService.persistCollection(new Collection(collection.name(), institutionName));
    }

    @DELETE
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
//    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Collection.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public void deleteCollection() {
        throw new UnsupportedOperationException("Not implemented");
    }


}
