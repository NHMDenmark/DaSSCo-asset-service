package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.GraphData;
import dk.northtech.dasscoassetservice.domain.StatisticsData;
import joptsimple.internal.Strings;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.map.ListOrderedMap;
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
import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

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
            new StatisticsData("InstituteTwo", "ti-pl-01", "ti-ws-01", dateTimeFormatter.parse("2022-04-30T12:11:25.7614801Z[UTC]", Instant::from).toEpochMilli(), 2),
            new StatisticsData("InstituteTwo", "ti-pl-02", "ti-ws-02", dateTimeFormatter.parse("2022-05-09T12:11:25.7614801Z[UTC]", Instant::from).toEpochMilli(), 2),
            new StatisticsData("InstituteOne", "ti-pl-01", "ti-ws-02", dateTimeFormatter.parse("2022-11-15T12:11:25.7614801Z[UTC]", Instant::from).toEpochMilli(), 1),
            new StatisticsData("InstituteOne", "ti-pl-02", "ti-ws-02", dateTimeFormatter.parse("2022-11-15T12:11:25.7614801Z[UTC]", Instant::from).toEpochMilli(), 2),
            new StatisticsData("InstituteOne", "ti-pl-01", "ti-ws-02", dateTimeFormatter.parse("2023-02-10T12:11:25.7614801Z[UTC]", Instant::from).toEpochMilli(), 1),
            new StatisticsData("InstituteOne", "ti-pl-01", "ti-ws-01", dateTimeFormatter.parse("2023-04-30T12:11:25.7614801Z[UTC]", Instant::from).toEpochMilli(), 2),
            new StatisticsData("InstituteOne", "ti-pl-01", "ti-ws-02", dateTimeFormatter.parse("2023-05-03T12:11:25.7614801Z[UTC]", Instant::from).toEpochMilli(), 1),
            new StatisticsData("InstituteOne", "ti-pl-02", "ti-ws-02", dateTimeFormatter.parse("2023-05-05T12:11:25.7614801Z[UTC]", Instant::from).toEpochMilli(), 2),
            new StatisticsData("InstituteTwo", "ti-pl-01", "ti-ws-02", dateTimeFormatter.parse("2023-05-05T12:11:25.7614801Z[UTC]", Instant::from).toEpochMilli(), 1),
            new StatisticsData("InstituteTwo", "ti-pl-02", "ti-ws-01", dateTimeFormatter.parse("2023-05-05T12:11:25.7614801Z[UTC]", Instant::from).toEpochMilli(), 2),
            new StatisticsData("InstituteOne", "ti-pl-01", "ti-ws-01", dateTimeFormatter.parse("2023-05-09T12:11:25.7614801Z[UTC]", Instant::from).toEpochMilli(), 1)
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
        generateIncrData(fullData, yearFormatter, isAfter);
    }

    @Test
    public void calculcateMonth() {
        Map<String, GraphData> fullData = new HashMap<>();
        DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(ZoneId.of("UTC"));
        Instant isAfter = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(1).toInstant();
        generateIncrData(fullData, yearFormatter, isAfter);
    }

    @Test
    public void calculcateYear() {
        DateTimeFormatter yearFormatter = new DateTimeFormatterBuilder() // MMM yyyy as it's also the label for the chart and it's only monthly
                .appendPattern("MMM yyyy")
                .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .toFormatter(Locale.ENGLISH)
                .withZone(ZoneId.of("UTC"));

        Map<String, GraphData> incrData = new HashMap<>();
        Instant isAfter = ZonedDateTime.now(ZoneOffset.UTC).minusYears(1).toInstant();

        generateIncrData(incrData, yearFormatter, isAfter);
        Map<String, GraphData> exponData = generateExponData(incrData, yearFormatter);

        Assertions.assertEquals(incrData.size(), exponData.size());
    }

    @Test
    public void testDateParsing() {
        long milliA = dateTimeFormatter.parse("2022-04-30T12:11:25.7614801Z[UTC]", Instant::from).toEpochMilli();
        long milliB = dateTimeFormatter.parse("2022-05-30T12:11:25.7614801Z[UTC]", Instant::from).toEpochMilli();

        DateTimeFormatter dtf = new DateTimeFormatterBuilder() // default day and hour as the pattern is only month and year
                .appendPattern("MMM yyyy")
                .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .toFormatter(Locale.ENGLISH)
                .withZone(ZoneId.of("UTC"));

        Instant createdDateA = Instant.ofEpochMilli(milliA);
        Instant createdDateB = Instant.ofEpochMilli(milliB);

        String dateStringA = dtf.format(createdDateA);
        String dateStringB = dtf.format(createdDateB);

        Assertions.assertEquals(dtf.parse(dateStringA, Instant::from).compareTo(dtf.parse(dateStringB, Instant::from)), -1);
        Assertions.assertEquals(dtf.parse(dateStringB, Instant::from).compareTo(dtf.parse(dateStringA, Instant::from)), 1);
    }

    public void generateIncrData(Map<String, GraphData> fullData, DateTimeFormatter dateFormatter, Instant isAFter) {
        testStatisticsData.forEach(data -> {
            Instant createdDate = Instant.ofEpochMilli(data.createdDate());
            if (createdDate.isAfter(isAFter)) {
                String dateString = dateFormatter.format(createdDate);
                if (!fullData.containsKey(dateString)) {
                    fullData.put(dateString, new GraphData(
                            new HashMap<>() {{put(data.instituteName(), data.specimens());}},
                            new HashMap<>() {{put(data.pipelineName(), data.specimens());}},
                            new HashMap<>() {{put(data.workstationName(), data.specimens());}}
                    ));
                } else {
                    updateData(fullData.get(dateString).getInstitutes(), data.instituteName(), data.specimens());
                    updateData(fullData.get(dateString).getPipelines(), data.pipelineName(), data.specimens());
                    updateData(fullData.get(dateString).getWorkstations(), data.workstationName(), data.specimens());
                }
            }
        });
    }

    public Map<String, GraphData> generateExponData(Map<String, GraphData> incrData, DateTimeFormatter dateFormatter) {
        // makes sure the map is sorted chronologically by date
        ListOrderedMap<String, GraphData> sortedData = incrData.entrySet()
                .stream()
                .sorted(Comparator.comparing(e -> dateFormatter.parse(e.getKey(), Instant::from)))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, ListOrderedMap::new));

        // then adds the values to the next map entry to get the exponential values
        MapIterator<String, GraphData> it = sortedData.mapIterator();
        while (it.hasNext()) {
            String key = it.next();
            GraphData value = it.getValue();
            if (!Strings.isNullOrEmpty(sortedData.nextKey(key))) { // if there's a next
                GraphData nextVal = sortedData.get(sortedData.nextKey(key));
                value.getInstitutes().keySet().forEach(instituteName -> nextVal.addInstituteAmts(instituteName, value.getInstitutes().get(instituteName)));
                value.getPipelines().keySet().forEach(pipelineName -> nextVal.addPipelineAmts(pipelineName, value.getPipelines().get(pipelineName)));
                value.getWorkstations().keySet().forEach(workstationName -> nextVal.addWorkstationAmts(workstationName, value.getWorkstations().get(workstationName)));
            }
        }
        return sortedData;
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
