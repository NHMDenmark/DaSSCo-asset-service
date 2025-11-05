package dk.northtech.dasscoassetservice.assets;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("frontend-properties")
public class FrontendProperties {
  public final String authenticationUrl;
  public final String rootUrl;
  public final String clientId;
  public final String fileProxyRootUrl;
  public final String wikiPageUrl;
  public final String externalLink;

  public FrontendProperties(String authenticationUrl, String rootUrl, String clientId, String fileProxyRootUrl, String wikiPageUrl, String externalLink) {
    this.authenticationUrl = authenticationUrl;
    this.rootUrl = rootUrl;
    this.clientId = clientId;
    this.fileProxyRootUrl = fileProxyRootUrl;
    this.wikiPageUrl = wikiPageUrl;
    this.externalLink = externalLink;
  }
}
