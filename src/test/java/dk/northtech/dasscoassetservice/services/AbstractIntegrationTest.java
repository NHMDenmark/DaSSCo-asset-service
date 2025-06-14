package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.amqp.AcknowledgeQueueListener;
import dk.northtech.dasscoassetservice.amqp.AssetQueueListener;
import dk.northtech.dasscoassetservice.amqp.QueueBroadcaster;
import dk.northtech.dasscoassetservice.amqp.QueueListener;
import dk.northtech.dasscoassetservice.configuration.Services;
import dk.northtech.dasscoassetservice.domain.MinimalAsset;
import dk.northtech.dasscoassetservice.domain.User;
import dk.northtech.dasscoassetservice.webapi.domain.HttpAllocationStatus;
import dk.northtech.dasscoassetservice.webapi.domain.HttpInfo;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
            .withEnv("POSTGRES_PASSWORD", "dassco_file_proxy")
            ;

    @Inject RightsValidationService rightsValidationService;
    @Inject InstitutionService institutionService;
    @Inject CollectionService collectionService;
    @Inject PipelineService pipelineService;
//    @Inject StatisticsDataService statisticsDataService;
    @Inject StatisticsDataServiceV2 statisticsDataServicev2;
    @Inject
    WorkstationService workstationService;
    @Inject InternalStatusService internalStatusService;

    @Inject BulkUpdateService bulkUpdateService;

    AssetService assetService;
    @Inject FileProxyClient fileProxyClient;
    @Inject SpecifyAdapterClient specifyAdapterClient;
    @Inject PublicationService publicationService;
    @Inject QueriesService queriesService;
    @Inject AssetGroupService assetGroupService;
    @Inject AssetSyncService assetSyncService;

    @Inject ExtendableEnumService extendableEnumService;
    @Inject SpecimenService specimenService;
    @Inject
    Jdbi jdbi;
    @Inject UserService userService;
    User user = new User();
    @Inject
    void setAssetService(AssetService assetService, AssetService assetService2) {
        AssetService spyAssetService = spy(assetService);
        HttpInfo success = new HttpInfo("/", "host.dk", 10000, 20000, 9990, 10, "success", HttpAllocationStatus.SUCCESS);
        doReturn(success).when(spyAssetService).openHttpShare(any(MinimalAsset.class), any(User.class), anyInt());

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);

        HttpClient mockHttpClient = mock(HttpClient.class);
        try {
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        doReturn(mockHttpClient).when(spyAssetService).createHttpClient();

        this.assetService = spyAssetService;
    }


    @MockitoBean
    QueueBroadcaster queueBroadcaster;

    @MockitoBean
    AssetQueueListener assetQueueListener;

    @MockitoBean
    Services services;

    @MockitoBean
    AcknowledgeQueueListener acknowledgeQueueListener;


    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        System.out.println("PORT "+ postgreSQL.getFirstMappedPort());
        // These tests assume the dev dataset, so roll that context on:
        registry.add("spring.liquibase.contexts", () -> "default, development, test");
        registry.add("datasource.jdbcUrl", () -> "jdbc:postgresql://localhost:" + postgreSQL.getFirstMappedPort() + "/dassco_file_proxy");
    }
}
