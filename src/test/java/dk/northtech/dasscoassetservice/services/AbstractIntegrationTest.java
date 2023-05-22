package dk.northtech.dasscoassetservice.services;

import jakarta.inject.Inject;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
@DirtiesContext
public class AbstractIntegrationTest {
    @Container
    static GenericContainer postgreSQL = new GenericContainer(DockerImageName.parse("apache/age:v1.1.0"))
            .withExposedPorts(5432)
            .withEnv("POSTGRES_DB", "dassco_asset_service")
            .withEnv("POSTGRES_USER", "dassco_asset_service")
            .withEnv("POSTGRES_PASSWORD", "dassco_asset_service");

    @Inject
    InstitutionService institutionService;

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        // These tests assume the dev dataset, so roll that context on:
        registry.add("spring.liquibase.contexts", () -> "default, development");
        registry.add("datasource.jdbcUrl", () -> "jdbc:postgresql://localhost:" + postgreSQL.getFirstMappedPort() + "/dassco_asset_service");
    }
}
