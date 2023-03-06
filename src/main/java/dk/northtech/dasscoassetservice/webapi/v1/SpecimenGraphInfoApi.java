package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.domain.SpecimenGraphInfo;
import dk.northtech.dasscoassetservice.services.SpecimenService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.springframework.stereotype.Component;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/v1/specimengraphinfo")
@SecurityRequirement(name = "dassco-idp")
public class SpecimenGraphInfoApi {
    private final SpecimenService specimenService;

    @Inject
    public SpecimenGraphInfoApi(SpecimenService specimenService) {
        this.specimenService = specimenService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = SpecimenGraphInfo.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<SpecimenGraphInfo> getAssets() {
        return specimenService.specimenDataByInstitute();
    }
}
