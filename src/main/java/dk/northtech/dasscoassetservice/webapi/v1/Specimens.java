package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.domain.Specimen;
import dk.northtech.dasscoassetservice.services.SpecimenService;
import dk.northtech.dasscoassetservice.services.UserService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
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

import java.util.ArrayList;
import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;


@Component
@Path("/v1/specimens/")
@Tag(name = "Specimens", description = "Endpoints related to collection specimens")
@SecurityRequirement(name = "dassco-idp")
public class Specimens {

    private SpecimenService specimenService;
    private UserService userService;

    @Inject
    public Specimens(SpecimenService specimenService, UserService userService) {
        this.specimenService = specimenService;
        this.userService = userService;
    }



    @PUT
    @Path("/{specimenPID}")
    @Operation(summary = "Update Specimen", description = "Update a specimen")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Specimen.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Specimen updateSpacemen(Specimen specimen
            , @PathParam("specimenPID") String specimenPID
            , @Context SecurityContext securityContext) {
        specimenService.putSpecimen(specimen, userService.from(securityContext));
        return specimen;
    }

    @GET
    @Path("/preparationTypes")
    @Operation(summary = "Get Preparation Type List")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = String.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<String> getPreparationTypes(){
        return this.specimenService.listPreparationTypes();
    }
}
