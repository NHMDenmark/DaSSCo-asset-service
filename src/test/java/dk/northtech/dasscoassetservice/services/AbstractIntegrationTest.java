package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.MinimalAsset;
import dk.northtech.dasscoassetservice.domain.User;
import dk.northtech.dasscoassetservice.webapi.domain.HttpAllocationStatus;
import dk.northtech.dasscoassetservice.webapi.domain.HttpInfo;
import jakarta.inject.Inject;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
@DirtiesContext
public class AbstractIntegrationTest {
    @Container
    static GenericContainer postgreSQL = new GenericContainer(DockerImageName.parse("apache/age:release_PG11_1.5.0"))
            .withExposedPorts(5432)
            .withEnv("POSTGRES_DB", "dassco_file_proxy")
            .withEnv("POSTGRES_USER", "dassco_file_proxy")
            .withEnv("POSTGRES_PASSWORD", "dassco_file_proxy");

    @Inject InstitutionService institutionService;
    @Inject PipelineService pipelineService;
    @Inject StatisticsDataService statisticsDataService;
    @Inject
    WorkstationService workstationService;
    @Inject InternalStatusService internalStatusService;
    AssetService assetService;
    @Inject FileProxyClient fileProxyClient;
    @Inject PublicationService publicationService;
    @Inject QueriesService queriesService;
    @Inject AssetGroupService assetGroupService;

    @Inject
    void setAssetService(AssetService assetService) {
        AssetService spyAssetService = spy(assetService);
        HttpInfo success = new HttpInfo("/", "host.dk", 10000, 20000, 9990, 10, "success", HttpAllocationStatus.SUCCESS);
        doReturn(success).when(spyAssetService).openHttpShare(any(MinimalAsset.class), any(User.class), anyInt());
        this.assetService = spyAssetService;
    }

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        // These tests assume the dev dataset, so roll that context on:
        registry.add("spring.liquibase.contexts", () -> "default, development, test");
        registry.add("datasource.jdbcUrl", () -> "jdbc:postgresql://localhost:" + postgreSQL.getFirstMappedPort() + "/dassco_file_proxy");
    }
}
