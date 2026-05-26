package dk.northtech.dasscoassetservice.configuration;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("fileproxy")
public record FileProxyConfiguration(String url, int shareCreationBlockedSeconds) {
    public FileProxyConfiguration {
        url = withoutTrailingSlash(url).concat("/file_proxy/api");
    }

    private static String withoutTrailingSlash(String s) {
        s = s.trim();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
