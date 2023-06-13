package dk.northtech.dasscoassetservice.webapi;

import dk.northtech.dasscoassetservice.domain.User;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Map;

public class UserMapper {
    public static User from(JwtAuthenticationToken token) {
        Map<String, Object> tokenAttributes = token.getTokenAttributes();
        User user = new User();
        user.keycloakId = String.valueOf(tokenAttributes.get("sub"));
        user.username = String.valueOf(tokenAttributes.get("preferred_username"));
        user.token = token.getToken().getTokenValue();
        System.out.println(token.getToken().getTokenValue());
        return user;
    }
}
