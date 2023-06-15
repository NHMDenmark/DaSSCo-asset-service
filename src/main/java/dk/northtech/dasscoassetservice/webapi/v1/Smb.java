package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.MinimalAsset;
import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.domain.User;
import dk.northtech.dasscoassetservice.services.AssetService;
import dk.northtech.dasscoassetservice.services.FileProxyClient;
import dk.northtech.dasscoassetservice.webapi.UserMapper;
import dk.northtech.dasscoassetservice.webapi.domain.AssetSmbRequest;
import dk.northtech.dasscoassetservice.webapi.domain.SambaInfo;
import dk.northtech.dasscoassetservice.webapi.domain.SmbRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/v1/shares/")
@SecurityRequirement(name = "dassco-idp")
public class Smb {
    public FileProxyClient fileProxyClient;
    public AssetService assetService;

    @Inject
    public Smb(FileProxyClient fileProxyClient, AssetService assetService) {
        this.fileProxyClient = fileProxyClient;
        this.assetService = assetService;
    }

    //Open a share for a single asset to upload media
    @POST
    @Path("/openAsset")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    public SambaInfo checkoutAsset(MinimalAsset asset, @Context SecurityContext securityContext) {
        JwtAuthenticationToken tkn = (JwtAuthenticationToken) securityContext.getUserPrincipal();
        User user = UserMapper.from(tkn);
        return fileProxyClient.openSamba(asset, user);
    }

    //Open a share with many assets
    @POST
    @Path("/open")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    public SambaInfo closeSmb(SmbRequest smbRequest
            , @Context SecurityContext securityContext) {
        JwtAuthenticationToken tkn = (JwtAuthenticationToken) securityContext.getUserPrincipal();
        User user = UserMapper.from(tkn);
        return fileProxyClient.openSamba(smbRequest, user);
    }

    @POST
    @Path("/disconnectShare")
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
    @Path("/closeShare")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    public SambaInfo closeSmb(AssetSmbRequest smbRequest
            , @QueryParam("syncERDA") boolean syncERDA
            , @Context SecurityContext securityContext) {
        JwtAuthenticationToken tkn = (JwtAuthenticationToken) securityContext.getUserPrincipal();
        User from = UserMapper.from(tkn);
        return fileProxyClient.closeSamba(from, smbRequest, syncERDA);
    }
}
