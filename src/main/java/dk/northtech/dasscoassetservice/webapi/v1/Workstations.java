package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.domain.Workstation;
import dk.northtech.dasscoassetservice.services.WorkstationService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.Operation;
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

@Component
@Path("/v1/institutions/{institutionName}/workstations")
@Tag(name = "Workstations", description = "Endpoints related to institutions workstations")
@SecurityRequirement(name = "dassco-idp")
public class Workstations {
    private WorkstationService workstationService;

    @Inject
    public Workstations(WorkstationService workstationService) {
        this.workstationService = workstationService;
    }

    @GET
    @Operation(summary = "List Workstations", description = "Lists workstations belonging to an institution.")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Workstation.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<Workstation> getWorkstations(@PathParam("institutionName") String institutionName) {
        return workstationService.listWorkstations(new Institution(institutionName));
    }

    @POST
    @Operation(summary = "Create Workstation", description = "Register a workstation in an institution.\n\n" +
                                                                "Workstation names must be unique.")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Workstation.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Workstation createWorkstation(@PathParam("institutionName") String institutionName, Workstation in) {
        return this.workstationService.createWorkStation(institutionName, in);
    }

    @PUT
    @Operation(summary = "Update Workstation", description = "Updates the status on a workstation.\n\n" +
                                                                "Valid statuses: \"IN_SERVICE\", \"OUT_OF_SERVICE\".")
    @Path("/{workstationName}")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "204", description = "No Content. The Update was successfull")
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public void updateWorkstation(@PathParam("institutionName") String institutionName, Workstation in) {
        this.workstationService.updateWorkstation(in, institutionName);
    }
}
