package dk.northtech.dasscoassetservice.configuration;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("specify-adapter")
public record SpecifyAdapterConfiguration(String url) {
    public SpecifyAdapterConfiguration {
        url = withoutTrailingSlash(url);
    }

    private static String withoutTrailingSlash(String s) {
        s = s.trim();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
