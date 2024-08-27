package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.services.MappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import javax.print.attribute.standard.Media;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Path("/v1/mappings")
@Tag(name = "Mappings", description = "Institutions and Collections may have many names in ARS. They need to be mapped to the names used in Specify. These endpoints save and retrieve the mappings for the Specify Adapter.")
@SecurityRequirement(name = "dassco-idp")
public class Mappings {

    MappingService mappingService;

    @Inject
    public Mappings(MappingService mappingService){
        this.mappingService = mappingService;
    }

    @POST
    @Path("/institutions")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Add Institutions to Mappings", description = "Adds Institutions to the mappings table. Takes a JSON Object, the Key should be the Specify Institution name, and the values is an array of Strings representing the different names that that institution has in the ARS.")
    public Response addInstitutionsToMapping(@RequestBody Map<String, List<String>> institutionMappings){
        return mappingService.addInstitutionsToMapping(institutionMappings);
    }

    @GET
    @Path("/institutions/{arsInstitution}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get ARS Institution Mapping", description = "Gets the Specify equivalent of the ARS Institution passed.")
    public Response getInstitutionMapping(@PathParam("arsInstitution") String arsInstitution){
        return mappingService.getInstitutionMapping(arsInstitution);
    }

    @PUT
    @Path("/institutions/specify")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Modify Specify Institution", description = "Changes the name for one or more Specify Institutions. Takes a Json Object. Keys are the old names, values are the new ones.")
    public Response updateSpecifyInstitutions(@RequestBody Map<String, String> institutions){
        return mappingService.updateSpecifyInstitutions(institutions);
    }

    @PUT
    @Path("/institutions/ars")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Modify ARS Institution", description = "Changes the name for one or more ARS Institutions. Takes a JSON Object. Keys are the old names, values are the new ones")
    public Response updateArsInstitutions(@RequestBody Map<String, String> institutions){
        return mappingService.updateArsInstitution(institutions);
    }

    @DELETE
    @Path("/institutions")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete Institution Mapping", description = "Deletes Institution Mappings. Takes a JSON Object. Keys are Specify Institutions, values are the ARS Institutions that the user wants to remove from the mapping.")
    public Response deleteInstitutionMapping(@RequestBody Map<String, List<String>> mappings){
        return mappingService.deleteMappings(mappings);
    }

    @POST
    @Path("/collections")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Add Collections to Mappings", description = "Adds Collections to the mappings table. Takes a JSON Object, the Key should be the Specify Collection name, and the value is an array of Strings representing the different names that that collection has in the ARS.")
    public Response addCollectionsToMapping(@RequestBody Map<String, List<String>> collectionMappings){
        return mappingService.addCollectionsToMapping(collectionMappings);
    }
}
