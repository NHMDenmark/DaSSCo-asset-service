package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.services.SpecimenService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@Path("/v1/assets")
@SecurityRequirement(name = "dassco-idp")
public class AssetApi {
    private final SpecimenService specimenService;

    @Inject
    public AssetApi(SpecimenService specimenService) {
        this.specimenService = specimenService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Asset.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<String> getAssets(@Context SecurityContext securityContext) {
        System.out.println(securityContext.getUserPrincipal().getName());
        System.out.println(securityContext.isUserInRole(SecurityRoles.ADMIN));
        List<Asset> list = new ArrayList<>();
        list.add(new Asset("",Instant.now(),"","","","","",Instant.now(),"",new ArrayList<>(),new ArrayList<>(),"","",Instant.now(),new ArrayList<>(),new ArrayList<>(),new ArrayList<>(),"","", Instant.now(),"","","","","","","","","","","","","","","","","","","","","","","","","","","","","",new ArrayList<>(),"","","","","","","",new ArrayList<>(),"",""));
        return specimenService.getSpecimenData();
//        return list;
    }
}
