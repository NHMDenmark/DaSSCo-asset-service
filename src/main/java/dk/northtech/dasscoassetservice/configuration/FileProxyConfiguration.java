package dk.northtech.dasscoassetservice.configuration;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("fileproxy")
public record FileProxyConfiguration(String url, int shareCreationBlockedSeconds) {

    public FileProxyConfiguration(String url, int shareCreationBlockedSeconds) {
        this.url = url +"/file_proxy/api";
        this.shareCreationBlockedSeconds = shareCreationBlockedSeconds;
    }
}
