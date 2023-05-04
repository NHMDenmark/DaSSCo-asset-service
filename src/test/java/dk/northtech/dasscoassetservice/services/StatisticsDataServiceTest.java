package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.GraphData;
import dk.northtech.dasscoassetservice.domain.StatisticsData;
import org.joda.time.Instant;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
@Testcontainers
@Sql("/test-data.sql")
public class StatisticsDataServiceTest {

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
    private StatisticsDataService specimenService;

    List<StatisticsData> testStatisticsData = Arrays.asList(
            new StatisticsData("TestInstitution", "ti-pl-01", "ti-ws-01", "2023-05-04T12:11:25.7614801Z[UTC]", 2),
            new StatisticsData("InstituteTwo", "ti-pl-01", "ti-ws-02", "2023-05-04T13:38:59.0322214Z[UTC]", 1),
            new StatisticsData("TestInstitution", "ti-pl-01", "ti-ws-02", "2023-05-04T13:38:59.0322214Z[UTC]", 1),
            new StatisticsData("InstituteTwo", "ti-pl-01", "ti-ws-02", "2023-05-04T13:44:13.4920451Z[UTC]", 2)
            );

    @Test
    public void getSpecimenData() {
        List<StatisticsData> data = specimenService.getGraphData();
//        data.forEach(System.out::println);
        assertNotEquals(data.size(), 0);
    }

    @Test
    public void calculateSpecimenAmount() {
        Map<String, List<GraphData>> dataMap = new HashMap<>();
        testStatisticsData.forEach(data -> {
            System.out.println(Instant.parse(data.createdDate().split("\\[")[0]));
        });
    }

//    getKey(specimen: SpecimenGraph, statValue: StatValue): string {
//        if (statValue === StatValue.INSTITUTE) return specimen.instituteName;
//        if (statValue === StatValue.PIPELINE) return specimen.pipelineName;
//        return specimen.workstationName;
//    }

}
