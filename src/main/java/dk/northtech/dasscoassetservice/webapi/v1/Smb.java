package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.domain.User;
import dk.northtech.dasscoassetservice.webapi.UserMapper;
import dk.northtech.dasscoassetservice.webapi.domain.AssetSmbRequest;
import dk.northtech.dasscoassetservice.webapi.domain.SambaInfo;
import dk.northtech.dasscoassetservice.webapi.domain.SmbRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/v1/shares")
@SecurityRequirement(name = "dassco-idp")
public class Smb {
    @POST
    @Path("/checkoutasset")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER})
    public SambaInfo checkoutAsset(AssetSmbRequest request) {
        return null;
    }

    @POST
    @Path("/disconnect")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER})
    public SambaInfo pauseSmb(AssetSmbRequest smbRequest
        , @Context SecurityContext securityContext
        , @Context HttpHeaders headers) {
        JwtAuthenticationToken tkn = (JwtAuthenticationToken) securityContext.getUserPrincipal();
        User user = UserMapper.from(tkn);
        return null;
    }


}
