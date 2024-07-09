package dk.northtech.dasscoassetservice.webapi.v1;

import com.google.inject.Inject;
import dk.northtech.dasscoassetservice.domain.Digitiser;
import dk.northtech.dasscoassetservice.domain.User;
import dk.northtech.dasscoassetservice.services.DigitiserService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.springframework.stereotype.Component;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/v1/digitisers")
@Tag(name = "Digitisers", description = "Endpoints related to digitisers")
@SecurityRequirement(name = "dassco-idp")
public class Digitisers {

    private DigitiserService digitiserService;

    @Inject
    public Digitisers(DigitiserService digitiserService){
        this.digitiserService = digitiserService;
    }

    @GET
    @Operation(summary = "Get Digitisers", description = "Lists the existing digitisers in the System")
    @Produces(MediaType.APPLICATION_JSON)
    // Roles allowed?
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Digitiser.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<Digitiser> getUsers(){
        return digitiserService.listDigitisers();
    }
}
