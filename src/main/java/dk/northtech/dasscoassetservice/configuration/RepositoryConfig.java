package dk.northtech.dasscoassetservice.configuration;

import dk.northtech.dasscoassetservice.repositories.DigitiserRepository;
import org.jdbi.v3.core.Jdbi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepositoryConfig {

    @Bean
    public DigitiserRepository digitiserRepository(Jdbi jdbi) {
        return jdbi.onDemand(DigitiserRepository.class);
    }
}
