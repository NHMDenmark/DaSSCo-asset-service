package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.services.InstitutionService;
import dk.northtech.dasscoassetservice.services.UserService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/v1/institutions")
@Tag(name = "Institutions", description = "Endpoints related to institutions")
@SecurityRequirement(name = "dassco-idp")
public class Institutions {
    private InstitutionService institutionService;
    private UserService userService;
    @Inject
    public Institutions(InstitutionService institutionService, UserService userService) {
        this.institutionService = institutionService;
        this.userService = userService;
    }

    @GET
    @Operation(summary = "Get Institution List", description = "Returns a list of institutions.")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Institution.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<Institution> getInstitutes() {
        return institutionService.listInstitutions();
    }

    @GET
    @Path("/{institutionName}")
    @Operation(summary = "Get Institution", description = "Returns an institution.")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Institution.class)))
    @ApiResponse(responseCode = "204", description = "No Content. Institution does not exist.")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Institution getInstitutes(@PathParam("institutionName") String institutionName) {
        Optional<Institution> instOpt = institutionService.getIfExists(institutionName);
        return instOpt.orElse(null);
    }

    @PUT
    @Path("/{institutionName}")
    @Operation(summary = "Update role restrictions on Institution", description = "Updates the role restrictions of the institution.")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Institution.class)))
    @ApiResponse(responseCode = "204", description = "No Content. Institution does not exist.")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Institution updateInstitution(Institution institution
            , @PathParam("institutionName") String institutionName) {
        return institutionService.updateInstitution(institution);
//        return instOpt.orElse(null);
    }

    @POST
    @Operation(summary = "Create Institution", description = """
        Registers a new institution.
        Institutions can have a list of roles, that restricts access to the assets within the institution.
        If an institution have the role restriction NHMD users with the role NHMD_WRITE has read/write access and users with the role NHMD_READ only have read access.
    """)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Institution.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Institution createInstitution(Institution in, @Context SecurityContext securityContext) {
        userService.from(securityContext);
        return institutionService.createInstitution(in);
    }
}
