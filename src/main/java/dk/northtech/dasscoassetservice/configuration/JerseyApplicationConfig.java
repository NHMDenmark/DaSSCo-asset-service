package dk.northtech.dasscoassetservice.configuration;

import dk.northtech.dasscoassetservice.webapi.v1.AssetApi;
import dk.northtech.dasscoassetservice.webapi.v1.OpenAPI;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.ApplicationPath;

@Component
@ApplicationPath("/api")
public class JerseyApplicationConfig extends ResourceConfig {
  public JerseyApplicationConfig() {
    // Activate the designated JaxRs classes with API endpoints:
    register(AssetApi.class);
    register(OpenAPI.class);

    register(RolesAllowedDynamicFeature.class);
    register(ClientAbortInterceptor.class);
  }
}
