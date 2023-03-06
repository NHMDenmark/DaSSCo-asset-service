package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.SpecimenGraphInfo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.*;

import javax.inject.Inject;
import java.util.List;

@SpringBootTest
@Testcontainers
@Sql("/test-data.sql")
public class SpecimenServiceTest {

    @Container
    static GenericContainer postgreSQL = new GenericContainer(DockerImageName.parse("apache/age:v1.1.0"))
            .withExposedPorts(5432)
            .withEnv("POSTGRES_DB", "dassco_asset_service")
            .withEnv("POSTGRES_USER", "dassco_asset_service")
            .withEnv("POSTGRES_PASSWORD", "dassco_asset_service");

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("datasource.jdbcUrl", () -> "jdbc:postgresql://localhost:" + postgreSQL.getFirstMappedPort() + "/dassco_asset_service?currentSchema=dassco");
    }

    @Inject
    private SpecimenService specimenService;

    @Test
    public void getSpecimensByInstitute() {
        List<SpecimenGraphInfo> data = specimenService.specimenDataByInstitute();
        assertNotEquals(data.size(), 0);
    }

    @Test
    public void getSpecimensByPipelilne() {
        List<SpecimenGraphInfo> data = specimenService.specimenDataByPipeline();
//        data.forEach(System.out::println);
        assertNotEquals(data.size(), 0);
    }

}
