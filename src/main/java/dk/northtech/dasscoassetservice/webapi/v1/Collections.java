package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.services.CollectionService;
import dk.northtech.dasscoassetservice.services.RightsValidationService;
import dk.northtech.dasscoassetservice.webapi.UserMapper;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Collections", description = "Endpoints related to collections.")
@Path("/v1/institutions/{institutionName}/collections/")
@SecurityRequirement(name = "dassco-idp")
public class Collections {
    private CollectionService collectionService;
    private RightsValidationService rightsValidationService;

    @Inject
    public Collections(CollectionService collectionService, RightsValidationService rightsValidationService) {
        this.collectionService = collectionService;
        this.rightsValidationService = rightsValidationService;
    }



    @GET
    @Operation(summary = "Get Collections", description = "List collections under a given institution.")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiOperation(value = "List collections",  notes = "Lists all collections that belongs to given institution")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Collection.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<Collection> getInstitutes(@PathParam("institutionName") String institutionName
            , @Context SecurityContext securityContext) {
        System.out.println(UserMapper.from(securityContext).username);
        System.out.println(UserMapper.from(securityContext).roles);
        rightsValidationService.checkReadRights(UserMapper.from(securityContext),institutionName);
        return this.collectionService.listCollections(new Institution(institutionName), UserMapper.from(securityContext));
    }

    @POST
    @Operation(summary = "Create Collection", description = "Creates a new collection under an institution.")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Collection.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Collection createInstitution(
            @PathParam("institutionName") String institutionName
            , Collection collection
            , @Context SecurityContext securityContext) {
        rightsValidationService.checkWriteRightsThrowing(UserMapper.from(securityContext), institutionName);
        return this.collectionService.persistCollection(collection);
    }

    @PUT
    @Path("/{collectionName}")
    @Operation(summary = "Update Collection", description = "Updates the Collection.")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Institution.class)))
    @ApiResponse(responseCode = "204", description = "No Content. Institution does not exist.")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Collection updateInstitution(Collection collection
            , @PathParam("institutionName") String institutionName
            , @PathParam("collectionName") String collectionName) {
        return collectionService.updateCollection(collection);
    }
    // Hidden until implemented
    @Hidden
    @DELETE
    @Operation(summary = "Delete Collection", description = "Deletes a collection from an institution.")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    //@ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Collection.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public void deleteCollection() {
        throw new UnsupportedOperationException("Not implemented");
    }


}
