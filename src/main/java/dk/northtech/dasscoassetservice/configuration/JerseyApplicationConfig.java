package dk.northtech.dasscoassetservice.configuration;

import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DasscoIllegalActionExceptionMapper;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.IllegalArguementExceptionMapper;
import dk.northtech.dasscoassetservice.webapi.v1.*;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.springframework.stereotype.Component;

@Component
@ApplicationPath("/api")
public class JerseyApplicationConfig extends ResourceConfig {
  public JerseyApplicationConfig() {
    // Activate the designated JaxRs classes with API endpoints:
    register(AssetApi.class);
    register(OpenAPI.class);
    register(StatisticsDataApi.class);
//    register(InstituteApi.class);
    register(Institutions.class);
    register(Specimens.class);
    register(Collections.class);
    register(RolesAllowedDynamicFeature.class);
    register(ClientAbortInterceptor.class);
    register(Pipelines.class);
    register(Workstations.class);
    register(Publishers.class);
    register(Assetupdates.class);
    register(IllegalArguementExceptionMapper.class);
    register(DasscoIllegalActionExceptionMapper.class);
    register(Smb.class);
  }
}
