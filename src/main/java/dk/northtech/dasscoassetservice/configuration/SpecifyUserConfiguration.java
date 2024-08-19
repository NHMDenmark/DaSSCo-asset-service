package dk.northtech.dasscoassetservice.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("specify-user")
public record SpecifyUserConfiguration(String password) {
}
