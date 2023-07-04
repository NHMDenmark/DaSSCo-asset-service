package dk.northtech.dasscoassetservice.webapi;

import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.domain.User;
import jakarta.ws.rs.core.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Map;

public class UserMapper {
    public static User from(SecurityContext securityContext) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) securityContext.getUserPrincipal();
        Map<String, Object> tokenAttributes = token.getTokenAttributes();
        User user = new User();
        if(securityContext.isUserInRole(SecurityRoles.ADMIN)) {
            user.roles.add(SecurityRoles.ADMIN);
        }
        if(securityContext.isUserInRole(SecurityRoles.USER)) {
            user.roles.add(SecurityRoles.USER);
        }
        if(securityContext.isUserInRole(SecurityRoles.DEVELOPER)) {
            user.roles.add(SecurityRoles.DEVELOPER);
        }
        if(securityContext.isUserInRole(SecurityRoles.SERVICE)) {
            user.roles.add(SecurityRoles.SERVICE);
        }
        System.out.println(securityContext.getClass());
        user.keycloakId = String.valueOf(tokenAttributes.get("sub"));
        user.username = String.valueOf(tokenAttributes.get("preferred_username"));
        user.token = token.getToken().getTokenValue();
        return user;
    }
}
