package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.services.MappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

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
    @Path("/institutions/add")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Add Institutions to Mappings", description = "Adds Institutions to the mappings table. Takes a JSON Object, the Key should be the Specify Institution name, and the values is an array of Strings representing the different names that that institution has in the ARS.")
    public void addInstitutionsToMapping(@RequestBody Map<String, List<String>> institutionMappings){
        mappingService.addInstitutionsToMapping(institutionMappings);
    }
}
