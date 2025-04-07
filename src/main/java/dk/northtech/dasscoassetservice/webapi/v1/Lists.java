package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.services.ExtendableEnumService;
import dk.northtech.dasscoassetservice.webapi.domain.ListEntry;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.stereotype.Component;

import java.util.Set;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@ApiOperation("Collection API")
@Component
@Tag(name = "Lists", description = "Endpoints for maintaining lists of constants")
@Path("/v1/lists/")
@SecurityRequirement(name = "dassco-idp")
public class Lists {
    private ExtendableEnumService extendableEnumService;

    @Inject
    public Lists(ExtendableEnumService extendableEnumService) {
        this.extendableEnumService = extendableEnumService;
    }


    @GET
    @Path("fileformats")
    @Operation(summary = "Get list of file formats", description = "Get valid file formats for assets")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiOperation(value = "List fileformats", notes = "Lists all file formats")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = String.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Set<String> getInstitutes() {
        return extendableEnumService.getFileFormats();
    }

    @POST
    @Operation(summary = "Add file_format", description = """
                Add a new file format to the list of valid file formats
            """)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response createInstitution(
            ListEntry fileFormat
    ) {
        this.extendableEnumService.persistEnum(ExtendableEnumService.ExtendableEnum.FILE_FORMAT, fileFormat.name());
        return Response.status(Response.Status.CREATED).build();
    }

//    @PUT
//    @Path("/{collectionName}")
//    @Operation(summary = "Update role restrictions on collection", description = "Updates the role restrictions on the collection")
//    @Produces(MediaType.APPLICATION_JSON)
//    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
//    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Collection.class)))
//    @ApiResponse(responseCode = "204", description = "No Content. Institution does not exist.")
//    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
//    public Collection updateInstitution(Collection collection
//            , @PathParam("institutionName") String institutionName
//            , @PathParam("collectionName") String collectionName) {
//        return collectionService.updateCollection(collection);
//    }

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
