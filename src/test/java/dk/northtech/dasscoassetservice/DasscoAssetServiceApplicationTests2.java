package dk.northtech.dasscoassetservice;

import dk.northtech.dasscoassetservice.services.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@DirtiesContext // The database container is torn down between tests, so the pool cannot be reused
class DasscoAssetServiceApplicationTests2 extends AbstractIntegrationTest {

//    @Container
//    static GenericContainer postgreSQL = new GenericContainer(DockerImageName.parse("postgres:13.3-alpine"))
//            .withExposedPorts(5432)
//            .withEnv("POSTGRES_DB", "dassco_asset_service")
//            .withEnv("POSTGRES_USER", "dassco_asset_service")
//            .withEnv("POSTGRES_PASSWORD", "dassco_asset_service");
//
//    @DynamicPropertySource
//    static void dataSourceProperties(DynamicPropertyRegistry registry) {
//        // These tests assume the dev dataset, so roll that context on:
//        //registry.add("spring.liquibase.contexts", () -> "dev");
//        registry.add("datasource.jdbcUrl", () -> "jdbc:postgresql://localhost:" + postgreSQL.getFirstMappedPort() + "/dassco_asset_service");
//    }

    @Test
    void contextLoads() {
    }

}
