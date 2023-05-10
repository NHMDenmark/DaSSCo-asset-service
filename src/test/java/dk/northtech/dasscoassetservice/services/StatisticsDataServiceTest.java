package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.GraphData;
import dk.northtech.dasscoassetservice.domain.StatisticsData;
import joptsimple.internal.Strings;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.junit.Ignore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
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

@SpringBootTest
@Testcontainers
//@Sql("/test-data.sql")
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
    private StatisticsDataService statisticsDataService;

    static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC"));

    @Test
    @Disabled // ignored until local database with test data works properly. (Tests work if you have a local db running though.)
    public void calculcateWeek() {
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(ZoneId.of("UTC"));
        long isAfter = Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli();
        Map<String, GraphData> data = statisticsDataService.generateIncrData(isAfter, dayFormatter);

//        assertNotEquals(data.size(), 0); // todo setup test database to ACTUALLY throw in data.
    }

    @Test
    @Disabled // ignored until local database with test data works properly. (Tests work if you have a local db running though.)
    public void calculcateMonth() {
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(ZoneId.of("UTC"));
        long isAfter = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(1).toEpochSecond();
        Map<String, GraphData> data = statisticsDataService.generateIncrData(isAfter, monthFormatter);

//        assertNotEquals(data.size(), 0); // todo setup test database to ACTUALLY throw in data.
    }

    @Test
    @Disabled // ignored until local database with test data works properly. (Tests work if you have a local db running though.)
    public void calculcateYear() {
        DateTimeFormatter yearFormatter = new DateTimeFormatterBuilder() // MMM yyyy as it's also the label for the chart and it's only monthly
                .appendPattern("MMM yyyy")
                .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .toFormatter(Locale.ENGLISH)
                .withZone(ZoneId.of("UTC"));

        Map<String, GraphData> incrData = new HashMap<>();
        long isAfter = ZonedDateTime.now(ZoneOffset.UTC).minusYears(1).toEpochSecond();

        Map<String, GraphData> incrDate = statisticsDataService.generateIncrData(isAfter, yearFormatter);
        List<Map<String, GraphData>> yearData = statisticsDataService.generateExponData(incrDate, yearFormatter);

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
}
