package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.Digitiser;
import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.domain.Workstation;
import dk.northtech.dasscoassetservice.services.*;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.stereotype.Component;

import java.util.Map;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/v1/caches")
@Tag(name = "Caches", description = "Endpoints related to the different caches in the System")
@SecurityRequirement(name = "dassco-idp")
public class Caches {

    private CacheService cacheService;

    @Inject
    public Caches(CacheService cacheService){
        this.cacheService = cacheService;
    }

    @GET
    @Operation(summary = "Get All Caches", description = "Lists all the caches available in the system for the dropdowns")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", description = "Successful operation",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Map<String, Object> getAllCaches(){
        return this.cacheService.getAllCaches();
    }

    @GET
    @Path("digitisers")
    @Operation(summary = "Get Digitiser Cache", description = "Lists the cached digitisers")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", description = "Successful operation",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Map<String, Digitiser> getDigitiserCache(){
        return this.cacheService.getDigitiserMap();
    }
}
