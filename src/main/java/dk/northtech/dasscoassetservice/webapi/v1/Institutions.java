package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.services.InstitutionService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/v1/institutions")
@SecurityRequirement(name = "dassco-idp")
public class Institutions {
    private InstitutionService institutionService;

    @Inject
    public Institutions(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER})
    @ApiOperation(value = "List all institutions")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Institution.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<Institution> getInstitutes() {
        return institutionService.listInstitutions();
    }

    @GET
    @Path("/{institutionName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Institution.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Institution getInstitutes(@PathParam("institutionName") String institutionName) {
        Optional<Institution> instOpt = institutionService.getIfExists(institutionName);
        return instOpt.orElse(null);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Register a new institution")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Institution.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Institution createInstitution(Institution in) {
        return institutionService.createInstitution(in);
    }
}
