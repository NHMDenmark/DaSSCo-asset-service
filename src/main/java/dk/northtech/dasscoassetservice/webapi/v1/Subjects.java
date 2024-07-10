package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.Digitiser;
import dk.northtech.dasscoassetservice.services.SubjectService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/v1/subjects")
@Tag(name = "Subjects", description = "Endpoints related to subjects")
@SecurityRequirement(name = "dassco-idp")
public class Subjects {

    private SubjectService subjectService;

    @Inject
    public Subjects(SubjectService subjectService){
        this.subjectService = subjectService;
    }

    @GET
    @Operation(summary = "Get Subjects", description = "Lists the existing asset subjects in the System")
    @Produces(MediaType.APPLICATION_JSON)
    // Roles allowed?
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = String.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<String> getSubjects(){
        return subjectService.listSubjects();
    }
}
