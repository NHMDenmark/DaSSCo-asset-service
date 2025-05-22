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
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = String.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Set<String> getInstitutions() {
        return extendableEnumService.getFileFormats();
    }

    @GET
    @Path("statuses")
    @Operation(summary = "Get list of asset statuses", description = "Get valid asset statuses")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = String.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Set<String> getStatuses() {
        return extendableEnumService.getStatuses();
    }

    @GET
    @Path("preparationtypes")
    @Operation(summary = "Get list of preparation types", description = "Get valid specimen preparation types")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = String.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Set<String> getPreparationTypes() {
        return extendableEnumService.getPreparation_types();
    }

    @GET
    @Path("issuecategories")
    @Operation(summary = "Get list of issue categories", description = "Get valid categories for issues")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = String.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Set<String> getIssueCategories() {
        return extendableEnumService.getIssueCategories();
    }

    @GET
    @Path("externalpublishers")
    @Operation(summary = "Get list of publishers", description = "Get valid publishers")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = String.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Set<String> getExternalPublishers() {
        return extendableEnumService.getExternalPublishers();
    }

    @POST
    @Path("fileformats")
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

    @POST
    @Operation(summary = "Add status", description = """
                Add a new status to the list of valid asset statuses
            """)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @Path("status")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response createStatus(
            ListEntry status
    ) {
        this.extendableEnumService.persistEnum(ExtendableEnumService.ExtendableEnum.STATUS, status.name());
        return Response.status(Response.Status.CREATED).build();
    }

    @POST
    @Operation(summary = "Add issue_category", description = """
                Add a new issue category to the list of valid issue categories
            """)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    @Path("issuecategories")
    public Response createIssueCategory (
            ListEntry issueCategory
    ) {
        this.extendableEnumService.persistEnum(ExtendableEnumService.ExtendableEnum.ISSUE_CATEGORY, issueCategory.name());
        return Response.status(Response.Status.CREATED).build();
    }

    @POST
    @Operation(summary = "Add preparation_type", description = """
                Add a new preparation_type to the list of valid preparation types
            """)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    @Path("preparationtypes")
    public Response createPreparationType (
            ListEntry preparation_type
    ) {
        this.extendableEnumService.persistEnum(ExtendableEnumService.ExtendableEnum.PREPARATION_TYPE, preparation_type.name());
        return Response.status(Response.Status.CREATED).build();
    }

    @POST
    @Operation(summary = "Add a publisher", description = """
                Add a new publisher to the list of valid publishers
            """)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    @Path("externalpublishers")
    public Response createPublishers (
            ListEntry preparation_type
    ) {
        this.extendableEnumService.persistEnum(ExtendableEnumService.ExtendableEnum.EXTERNAL_PUBLISHER, preparation_type.name());
        return Response.status(Response.Status.CREATED).build();
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
