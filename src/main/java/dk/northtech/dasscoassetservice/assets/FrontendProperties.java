package dk.northtech.dasscoassetservice.assets;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("frontend-properties")
public class FrontendProperties {
  public final String authenticationUrl;
  public final String apiUrl;
  public final String clientId;
  public final String apiUrlComp;
  public final String apiUrlClimbalong;
  public final String datalakeUrl;

  public FrontendProperties(String authenticationUrl, String apiUrl, String clientId, String apiUrlComp, String apiUrlClimbalong, String datalakeUrl) {
    this.authenticationUrl = authenticationUrl;
    this.apiUrl = apiUrl;
    this.clientId = clientId;
    this.apiUrlComp = apiUrlComp;
    this.apiUrlClimbalong = apiUrlClimbalong;
    this.datalakeUrl = datalakeUrl;
  }
}
