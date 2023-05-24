package dk.northtech.dasscoassetservice.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dk.northtech.dasscoassetservice.domain.GraphData;
import dk.northtech.dasscoassetservice.domain.GraphView;
import dk.northtech.dasscoassetservice.domain.StatisticsData;
import joptsimple.internal.Strings;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

//@SpringBootTest
//@Testcontainers
//@Sql("/test-data.sql")
public class StatisticsDataServiceTest extends AbstractIntegrationTest {

//    @Container
//    static GenericContainer postgreSQL = new GenericContainer(DockerImageName.parse("apache/age:v1.1.0"))
//            .withExposedPorts(5432)
//            .withEnv("POSTGRES_DB", "dassco_asset_service")
//            .withEnv("POSTGRES_USER", "dassco_asset_service")
//            .withEnv("POSTGRES_PASSWORD", "dassco_asset_service");

//    @DynamicPropertySource
//    static void dataSourceProperties(DynamicPropertyRegistry registry) {
//        registry.add("datasource.jdbcUrl", () -> "jdbc:postgresql://localhost:" + postgreSQL.getFirstMappedPort() + "/dassco_asset_service?currentSchema=dassco");
//    }

    @Inject
    private StatisticsDataService statisticsDataService;

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
    @Disabled // ignored until local database with test data works properly. (Tests work if you have a local db running though.)
    public void calculcateWeek() {
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(ZoneId.of("UTC"));
        long isAfter = Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli();
//        Map<String, GraphData> data = statisticsDataService.generateIncrData(isAfter, dayFormatter);
        Map<String, GraphData> data2 = generateIncrData(Instant.ofEpochMilli(isAfter), Instant.now(), dayFormatter, GraphView.WEEK);

//        assertNotEquals(data.size(), 0); // todo setup test database to ACTUALLY throw in data.
    }

    @Test
//    @Disabled // ignored until local database with test data works properly. (Tests work if you have a local db running though.)
    public void calculcateMonth() {
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(ZoneId.of("UTC"));
        Instant isAfter = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(1).toInstant();
//        Map<String, GraphData> data = statisticsDataService.generateIncrData(isAfter, Instant.now(), monthFormatter);
        Map<String, GraphData> data2 = generateIncrData(isAfter, Instant.now(), monthFormatter, GraphView.MONTH);

//        assertNotEquals(data.size(), 0); // todo setup test database to ACTUALLY throw in data.
    }

    @Test
//    @Disabled // ignored until local database with test data works properly. (Tests work if you have a local db running though.)
    public void calculcateYear() {
        DateTimeFormatter yearFormatter = new DateTimeFormatterBuilder() // MMM yyyy as it's also the label for the chart and it's only monthly
                .appendPattern("MMM yyyy")
                .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .toFormatter(Locale.ENGLISH)
                .withZone(ZoneId.of("UTC"));

        Map<String, GraphData> incrData = new HashMap<>();
        Instant isAfter = ZonedDateTime.now(ZoneOffset.UTC).minusYears(1).toInstant();

//        Map<String, GraphData> incrDate = statisticsDataService.generateIncrData(isAfter, Instant.now(), yearFormatter, GraphView.YEAR);
//        Map<String, Map<String, GraphData>> yearData = statisticsDataService.generateExponData(incrDate, yearFormatter);

//        Map<String, GraphData> data2 = generateIncrData(isAfter, Instant.now(), yearFormatter, GraphView.YEAR);

//        Assertions.assertEquals(yearData.get(0).isEmpty(), false); // todo setup test database to ACTUALLY throw in data.
    }

    @Test
    public void testDateParsing() { // testing of the parsing of the dates in the exponential calculations
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

    public Map<String, GraphData> generateIncrData(Instant startDate, Instant endDate, DateTimeFormatter dateTimeFormatter, GraphView timeFrame) {
        List<StatisticsData> statisticsData = testStatisticsData;
        Map<String, GraphData> incrData = new HashMap<>();
        List<String> dates = new ArrayList<>(); // dates = labels
        ChronoUnit unit = null;

        if (timeFrame.equals(GraphView.WEEK) || timeFrame.equals(GraphView.MONTH)) {
            unit = ChronoUnit.DAYS;
        }
        if (timeFrame.equals(GraphView.YEAR)) {
            unit = ChronoUnit.MONTHS;
        }

        long difference = unit.between(LocalDateTime.ofInstant(startDate, dateTimeFormatter.getZone()), LocalDateTime.ofInstant(endDate, dateTimeFormatter.getZone()));

        for (long i = difference; i >= 0; i--) {
            dates.add(dateTimeFormatter.format(LocalDateTime.ofInstant(endDate, dateTimeFormatter.getZone()).minus(i, unit)));
        }

        System.out.println(dates);

        statisticsData.forEach(data -> {
            Instant createdDate = Instant.ofEpochMilli(data.createdDate());
            String dateString = dateTimeFormatter.format(createdDate);
            if (!incrData.containsKey(dateString)) {
                incrData.put(dateString, new GraphData(
                        new HashMap<>() {{put(data.instituteName(), data.specimens());}},
                        new HashMap<>() {{put(data.pipelineName(), data.specimens());}},
                        new HashMap<>() {{put(data.workstationName(), data.specimens());}}
                ));
            } else {
                updateData(incrData.get(dateString).getInstitutes(), data.instituteName(), data.specimens());
                updateData(incrData.get(dateString).getPipelines(), data.pipelineName(), data.specimens());
                updateData(incrData.get(dateString).getWorkstations(), data.workstationName(), data.specimens());
            }
        });

        dates.forEach(date -> {
            if (!incrData.containsKey(date)) {
                incrData.put(date, new GraphData(new HashMap<>(), new HashMap<>(), new HashMap<>()));
            }
        });

        return incrData;
    }

    public List<Map<String, GraphData>> generateExponData(Map<String, GraphData> originalData, DateTimeFormatter dateFormatter) {
        Gson gson = new Gson(); // not a huge fan of this, but is the only way I can see - for now - to deep clone the map.
        String jsonString = gson.toJson(originalData);
        Type type = new TypeToken<HashMap<String, GraphData>>(){}.getType();
        Map<String, GraphData> deepClonedData = gson.fromJson(jsonString, type);

        // makes sure the map is sorted chronologically by date
        ListOrderedMap<String, GraphData> exponData = deepClonedData.entrySet()
                .stream()
                .sorted(Comparator.comparing(e -> dateFormatter.parse(e.getKey(), Instant::from)))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, ListOrderedMap::new));

        // then adds the values to the next map entry to get the exponential values
        MapIterator<String, GraphData> it = exponData.mapIterator();
        while (it.hasNext()) {
            String key = it.next();
            GraphData value = it.getValue();
            if (!Strings.isNullOrEmpty(exponData.nextKey(key))) { // if there's a next
                GraphData nextVal = exponData.get(exponData.nextKey(key));
                value.getInstitutes().keySet().forEach(instituteName -> nextVal.addInstituteAmts(instituteName, value.getInstitutes().get(instituteName)));
                value.getPipelines().keySet().forEach(pipelineName -> nextVal.addPipelineAmts(pipelineName, value.getPipelines().get(pipelineName)));
                value.getWorkstations().keySet().forEach(workstationName -> nextVal.addWorkstationAmts(workstationName, value.getWorkstations().get(workstationName)));
            }
        }

        return Arrays.asList(originalData, exponData);
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
