package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.services.SpecimenService;
import dk.northtech.dasscoassetservice.services.UserService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.springframework.stereotype.Component;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;


@Component
@Path("/v1/institutions/{institution}/collections/{collection}/specimen/{barcode}")
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
//    @Path("/institutions/{institution}/collections/{collection}/specimen/{barcode}")
    @Operation(summary = "Update Specimen", description = "Update a specimen")
    @Produces(MediaType.APPLICATION_JSON)
//    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Specimen.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Specimen updateSpacemen(@PathParam("institution") String institution
            , @PathParam("collection") String collection
            , @PathParam("barcode") String barcode
            , Specimen specimen
            , @Context SecurityContext securityContext) {
        System.out.println("testtesttest");
        if(!institution.equals(specimen.institution()) || !collection.equals(specimen.collection()) || !barcode.equals(specimen.barcode())) {
            throw new IllegalArgumentException("Specimen institution, collection and barcode in object must match path");
        }
        return specimenService.putSpecimen(specimen, userService.from(securityContext));
    }

    @GET
//    @Path("/institutions/{institution}/collections/{collection}/specimen/{barcode}")
    @Operation(summary = "Get specimen", description = "Get a specimen by institution, collection and barcode")
    @Produces(MediaType.APPLICATION_JSON)
    public Specimen getSpecimen(@PathParam("institution") String institution
            , @PathParam("collection") String collection
            , @PathParam("barcode") String barcode
            , @Context SecurityContext securityContext){
        User user = securityContext.getUserPrincipal() == null ? new User("anonymous") : userService.from(securityContext);
        return specimenService.findSpecimen(institution, collection, barcode, user)
                .orElseThrow(() -> new NotFoundException("No specimen found with institution " + institution + ", collection " + collection + " and barcode " + barcode));
    }

    @DELETE
//    @Path("/institutions/{institution}/collections/{collection}/specimen/{barcode}")
    @Operation(summary = "Delete specimen", description = "Delete a specimen by institution, collection and barcode")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = TEXT_PLAIN, schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "403", content = @Content(mediaType = TEXT_PLAIN, schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "404", content = @Content(mediaType = TEXT_PLAIN, schema = @Schema(implementation = String.class)))
    public Response deleteSpecimen(@PathParam("institution") String institution
            , @PathParam("collection") String collection
            , @PathParam("barcode") String barcode
            , @Context SecurityContext securityContext){
        return specimenService.deleteSpecimen(institution, collection, barcode, userService.from(securityContext));
    }

//    @GET
//    @Path("/specimens/preparationTypes")
//    @Operation(summary = "Get Preparation Type List")
//    @Produces(MediaType.APPLICATION_JSON)
//    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = String.class))))
//    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
//    public List<String> getPreparationTypes(){
//        return this.specimenService.listPreparationTypes();
//    }
}
