package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.configuration.SpecifyUserConfiguration;
import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.domain.SpecifyUser;
import dk.northtech.dasscoassetservice.services.SpecifyUserService;
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
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/v1/specify-users")
@Tag(name = "Pipelines", description = "Endpoints related to specify users used by the Specify Adapter")
@SecurityRequirement(name = "dassco-idp")
public class SpecifyUsers {
    private static final Logger logger = LoggerFactory.getLogger(SpecifyUsers.class);
    private SpecifyUserService specifyUserService;
    private SpecifyUserConfiguration specifyUserConfiguration;

    @Inject
    public SpecifyUsers(SpecifyUserService specifyUserService, SpecifyUserConfiguration specifyUserConfiguration) {
        this.specifyUserService = specifyUserService;
        this.specifyUserConfiguration = specifyUserConfiguration;
    }

    @POST
    @Path("/")
    @Operation(summary = "Create Specify User", description = "Creates a new Specify User connected to an institution.")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = SpecifyUser.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response newSpecifyUser(SpecifyUser specifyUser) {
        Optional<SpecifyUser> user = specifyUserService.newSpecifyUser(specifyUser);
        if (user.isPresent()) {
            return Response.status(Response.Status.OK).entity(user.get()).build();
        }
        return Response.status(Response.Status.CONFLICT).entity("ERROR: Institution " + specifyUser.institution() + " already has a user connected.").build();
    }

    @GET
    @Path("/{institutionName}")
    @Operation(summary = "Get credentials", description = "Gets the Specify credentials for an institution.")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response getSpecifyUser(@PathParam("institutionName") String institutionName) {
        Optional<SpecifyUser> user = specifyUserService.getUserFromInstitution(institutionName);
        String pass = specifyUserConfiguration.password(); // temp
        if (user.isPresent()) {
            return Response.status(Response.Status.OK).entity(pass).build();
        }
        return Response.status(Response.Status.NOT_FOUND).entity("No user found for institution " + institutionName).build();
    }

    @DELETE
    @Path("/{institutionName}")
    @Operation(summary = "Delete Specify User", description = "Removes the Specify User connected to the institution.")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response deleteSpecifyUser(@PathParam("institutionName") String institutionName) {
        Optional<SpecifyUser> deletedUser = specifyUserService.deleteUser(institutionName);
        if (deletedUser.isPresent()) {
            return Response.status(Response.Status.OK).entity("User \"" + deletedUser.get().username() + "\" connected to institution " + institutionName + " has been deleted.").build();
        }
        return Response.status(Response.Status.NOT_FOUND).entity("No user found for institution " + institutionName).build();
    }

    @PUT
    @Path("/{institutionName}")
    @Operation(summary = "Update Specify User", description = "Updates the Specify User connected to the institution.")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = SpecifyUser.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response updateSpecifyUser(@PathParam("institutionName") String institutionName, SpecifyUser specifyUser) {
        Optional<SpecifyUser> updatedUser = specifyUserService.updateUser(institutionName, specifyUser);
        if (updatedUser.isPresent()) {
            return Response.status(Response.Status.OK).entity(updatedUser.get()).build();
        }
        return Response.status(Response.Status.NOT_FOUND).entity("No user found to update for institution " + institutionName).build();
    }
}
