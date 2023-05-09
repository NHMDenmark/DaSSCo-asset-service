package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.GraphData;
import dk.northtech.dasscoassetservice.domain.StatisticsData;
import org.junit.Ignore;
import org.junit.jupiter.api.Assertions;
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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

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

    static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC"));
    List<StatisticsData> testStatisticsData = Arrays.asList(
            new StatisticsData("InstituteTwo", "ti-pl-01", "ti-ws-01", "2022-04-30T12:11:25.7614801Z[UTC]", 2),
            new StatisticsData("InstituteTwo", "ti-pl-02", "ti-ws-02", "2022-05-09T12:11:25.7614801Z[UTC]", 2),
            new StatisticsData("InstituteOne", "ti-pl-01", "ti-ws-02", "2022-11-15T12:11:25.7614801Z[UTC]", 1),
            new StatisticsData("InstituteOne", "ti-pl-02", "ti-ws-02", "2022-11-15T12:11:25.7614801Z[UTC]", 2),
            new StatisticsData("InstituteOne", "ti-pl-01", "ti-ws-02", "2023-02-10T12:11:25.7614801Z[UTC]", 1),
            new StatisticsData("InstituteOne", "ti-pl-01", "ti-ws-01", "2023-04-30T12:11:25.7614801Z[UTC]", 2),
            new StatisticsData("InstituteOne", "ti-pl-01", "ti-ws-02", "2023-05-03T12:11:25.7614801Z[UTC]", 1),
            new StatisticsData("InstituteOne", "ti-pl-02", "ti-ws-02", "2023-05-05T12:11:25.7614801Z[UTC]", 2),
            new StatisticsData("InstituteTwo", "ti-pl-01", "ti-ws-02", "2023-05-05T12:11:25.7614801Z[UTC]", 1),
            new StatisticsData("InstituteTwo", "ti-pl-02", "ti-ws-01", "2023-05-05T12:11:25.7614801Z[UTC]", 2),
            new StatisticsData("InstituteOne", "ti-pl-01", "ti-ws-01", "2023-05-09T12:11:25.7614801Z[UTC]", 1)
           );

    @Test
    @Ignore
    public void getSpecimenData() {
        List<StatisticsData> data = specimenService.getGraphData();
        assertNotEquals(data.size(), 0);
    }

    @Test
    public void calculcateWeek() {
        Map<String, GraphData> fullData = new HashMap<>();
        DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(ZoneId.of("UTC"));
        Instant isAfter = Instant.now().minus(7, ChronoUnit.DAYS);
        generateData(fullData, yearFormatter, isAfter);
    }

    @Test
    public void calculcateMonth() {
        Map<String, GraphData> fullData = new HashMap<>();
        DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(ZoneId.of("UTC"));
        Instant isAfter = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(1).toInstant();
        generateData(fullData, yearFormatter, isAfter);
    }

    @Test
    public void calculcateYear() {
        Map<String, GraphData> fullData = new HashMap<>();
        DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("MMM yyyy").withZone(ZoneId.of("UTC"));
        Instant isAfter = ZonedDateTime.now(ZoneOffset.UTC).minusYears(1).toInstant();
        generateData(fullData, yearFormatter, isAfter);
    }

    public void generateData(Map<String, GraphData> fullData, DateTimeFormatter dateFormatter, Instant isAFter) {

        testStatisticsData.forEach(data -> {
            Instant createdDate = dateTimeFormatter.parse(data.createdDate(), Instant::from);
            if (createdDate.isAfter(isAFter)) {
                String dateString = dateFormatter.format(createdDate);
                if (!fullData.containsKey(dateString)) {
                    fullData.put(dateString, new GraphData(
                            new HashMap<>() {{put(data.instituteName(), data.specimens());}},
                            new HashMap<>() {{put(data.pipelineName(), data.specimens());}},
                            new HashMap<>() {{put(data.workstationName(), data.specimens());}}
                    ));
                } else {
                    updateData(fullData.get(dateString).getInstitutions(), data.instituteName(), data.specimens());
                    updateData(fullData.get(dateString).getPipelines(), data.pipelineName(), data.specimens());
                    updateData(fullData.get(dateString).getWorkstations(), data.workstationName(), data.specimens());
                }
            }
        });

        System.out.println(fullData);
    }

    public void updateData(Map<String, Integer> existing, String key, Integer specimens) {
        if (existing.containsKey(key)) {
            int existingVal = existing.get(key);
            existing.put(key, existingVal + specimens);
        } else {
            existing.put(key, specimens);
        }
    }
}
