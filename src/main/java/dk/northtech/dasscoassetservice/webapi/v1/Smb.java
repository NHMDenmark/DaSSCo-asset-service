package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.domain.User;
import dk.northtech.dasscoassetservice.services.FileProxyClient;
import dk.northtech.dasscoassetservice.webapi.UserMapper;
import dk.northtech.dasscoassetservice.webapi.domain.AssetSmbRequest;
import dk.northtech.dasscoassetservice.webapi.domain.SambaInfo;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/v1/shares")
@SecurityRequirement(name = "dassco-idp")
public class Smb {
    public FileProxyClient fileProxyClient;

    @Inject
    public Smb(FileProxyClient fileProxyClient) {
        this.fileProxyClient = fileProxyClient;
    }


    @POST
    @Path("/checkoutasset")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    public SambaInfo checkoutAsset(AssetSmbRequest request) {
        return null;
    }

    @POST
    @Path("/disconnect")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    public SambaInfo pauseSmb(AssetSmbRequest smbRequest
        , @Context SecurityContext securityContext) {
        JwtAuthenticationToken tkn = (JwtAuthenticationToken) securityContext.getUserPrincipal();
        User from = UserMapper.from(tkn);
        return fileProxyClient.disconnectSamba(from, smbRequest);
    }

    @POST
    @Path("/close")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    public SambaInfo closeSmb(AssetSmbRequest smbRequest
            , @Context SecurityContext securityContext) {
        JwtAuthenticationToken tkn = (JwtAuthenticationToken) securityContext.getUserPrincipal();
        User from = UserMapper.from(tkn);
        return fileProxyClient.disconnectSamba(from, smbRequest);
    }


}
