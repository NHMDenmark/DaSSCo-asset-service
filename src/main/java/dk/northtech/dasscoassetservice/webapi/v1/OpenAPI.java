package dk.northtech.dasscoassetservice.webapi.v1;


import dk.northtech.dasscoassetservice.configuration.AuthConfiguration;
import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@OpenAPIDefinition(
        info = @Info(
                title = "DaSSCo",
                version = "1.0",
                description = """
      DaSSCO API Documentation
      """
        ),
        servers = {
                @Server(url = "${apiServerUrl}"),
        }
)
@SecurityScheme(
        name = "dassco-idp",
        type = SecuritySchemeType.OAUTH2,
        extensions = {
                @Extension(properties = @ExtensionProperty(name = "client-id", value = "${authClientId}")),
                @Extension(properties = @ExtensionProperty(name = "receive-token-in", value = "request-body"))
        },
        flows = @OAuthFlows(
                authorizationCode = @OAuthFlow(
                        authorizationUrl = "${authServerUrl}/protocol/openid-connect/auth",
                        tokenUrl = "${authServerUrl}/protocol/openid-connect/token"
                )
        )
)
@Path("/")
public class OpenAPI {
    private final AuthConfiguration authConfiguration;

    @Inject
    public OpenAPI(AuthConfiguration authConfiguration) {
        this.authConfiguration = authConfiguration;
    }

    @GET
    @Hidden
    @Path("openapi.json")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    public String json(@Context HttpServletRequest request) {
        return replaceServers(request, readFromClasspath("/openapi.json"));
    }

    @GET
    @Hidden
    @Path("openapi.yaml")
    @Produces("text/plain; charset=utf-8")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    public String yaml(@Context HttpServletRequest request) {
        return replaceServers(request, readFromClasspath("/openapi.yaml"));
    }

    static String readFromClasspath(String resourceName) {
        try (var in = OpenAPI.class.getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new UncheckedIOException(new IOException("No " + resourceName + " in classpath"));
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException iox) {
            throw new UncheckedIOException(iox);
        }
    }

    private String replaceServers(HttpServletRequest request, String s) {
        if (request != null) {
            var server = request.getScheme() + "://" + request.getServerName();
            if (request.getServerPort() != 80) {
                server += ":" + request.getServerPort();
            }

            var authServerUrlPattern = Pattern.compile("\\$\\{authServerUrl}");
            var clientIdPattern = Pattern.compile("\\$\\{authClientId}");
            var apiServerUrlPattern = Pattern.compile("\\$\\{apiServerUrl}");

            s = authServerUrlPattern.matcher(s).replaceAll(this.authConfiguration.serverUrl());
            s = clientIdPattern.matcher(s).replaceAll(this.authConfiguration.clientName());
            s = apiServerUrlPattern.matcher(s).replaceAll("http://localhost:8084/api");
        }
        return s;
    }
}
