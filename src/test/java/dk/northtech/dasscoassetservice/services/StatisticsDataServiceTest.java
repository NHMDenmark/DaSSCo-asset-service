package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

public class StatisticsDataServiceTest extends AbstractIntegrationTest {
    User user = new User();
    @Test
    public void calculcateWeek() {
        Asset createAsset = getTestAsset("week-asset");
        assetService.persistAsset(createAsset, user);

        Instant startDate = ZonedDateTime.now(ZoneOffset.UTC).minusWeeks(1).toInstant();
        String currentDate = getDateFormatter("dd-MMM-yyyy").format(Instant.now());
        long duration = ChronoUnit.DAYS.between(startDate, Instant.now()) + 1; // plus 1 as it doesn't count the first date as "between"

        Map<String, Map<String, GraphData>> finalData = statisticsDataService.getCachedGraphData(GraphView.WEEK);

        assertThat(finalData).containsKey("incremental");
        assertThat(finalData.get("incremental")).isNotEmpty();
        assertThat(finalData.get("incremental")).containsKey(currentDate);
        assertThat(finalData.get("incremental").get(currentDate).getInstitutes().get("institution_1")).isEqualTo(2);
        assertThat(finalData.get("incremental").size()).isEqualTo(duration);

        Map.Entry<String, GraphData> firstEntry = finalData.get("incremental").entrySet().iterator().next();
        assertThat(firstEntry.getKey()).isEqualTo(getDateFormatter("dd-MMM-yyyy").format(startDate));
    }

    @Test
    public void calculcateMonth() {
        Asset createAsset = getTestAsset("month-asset");
        assetService.persistAsset(createAsset, user);

        Instant startDate = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(1).toInstant();
        String currentDate = getDateFormatter("dd-MMM-yyyy").format(Instant.now());
        long duration = ChronoUnit.DAYS.between(startDate, Instant.now()) + 1; // plus 1 as it doesn't count the first date as "between"

        Map<String, Map<String, GraphData>> finalData = statisticsDataService.getCachedGraphData(GraphView.MONTH);

        assertThat(finalData).containsKey("incremental");
        assertThat(finalData.get("incremental")).isNotEmpty();
        assertThat(finalData.get("incremental")).containsKey(currentDate);
        assertThat(finalData.get("incremental").get(currentDate).getInstitutes().get("institution_1")).isEqualTo(2);
        assertThat(finalData.get("incremental").size()).isEqualTo(duration);

        Map.Entry<String, GraphData> firstEntry = finalData.get("incremental").entrySet().iterator().next();
        assertThat(firstEntry.getKey()).isEqualTo(getDateFormatter("dd-MMM-yyyy").format(startDate));
    }

    @Test
    public void calculcateYear() {
        Asset testAsset = getTestAsset("year-asset");
        assetService.persistAsset(testAsset, user);

        Instant startDateInstant = ZonedDateTime.now(ZoneOffset.UTC).minusYears(1).toInstant();
        String startDate = getDateFormatter("MMM yyyy").format(startDateInstant);
        String currentDate = getDateFormatter("MMM yyyy").format(Instant.now());

        long duration = ChronoUnit.MONTHS.between(
                LocalDate.parse(getDateFormatter("yyyy-MM-dd").format(startDateInstant)).withDayOfMonth(1),
                LocalDate.parse(getDateFormatter("yyyy-MM-dd").format(Instant.now())).withDayOfMonth(1)
        ) + 1; // plus 1 as it doesn't count the first month as "between";

        Map<String, Map<String, GraphData>> finalData = statisticsDataService.getCachedGraphData(GraphView.YEAR);

        assertThat(finalData).containsKey("incremental");
        assertThat(finalData.get("incremental")).isNotEmpty();
        assertThat(finalData.get("incremental")).containsKey(currentDate);
        assertThat(finalData.get("incremental").get(currentDate).getInstitutes().get("institution_1")).isEqualTo(2);
        assertThat(finalData.get("incremental").size()).isEqualTo(duration);

        assertThat(finalData).containsKey("exponential");
        assertThat(finalData.get("exponential")).isNotEmpty();
        assertThat(finalData.get("exponential")).containsKey(currentDate);
        assertThat(finalData.get("exponential").get(currentDate).getPipelines().get("i1_p1")).isEqualTo(2);
        assertThat(finalData.get("exponential").size()).isEqualTo(duration);

        Map.Entry<String, GraphData> firstEntryIncr = finalData.get("incremental").entrySet().iterator().next();
        Map.Entry<String, GraphData> firstEntryExpon = finalData.get("exponential").entrySet().iterator().next();
        assertThat(firstEntryIncr.getKey()).isEqualTo(startDate);
        assertThat(firstEntryExpon.getKey()).isEqualTo(startDate);
    }

    @Test
    public void calculcateCachedWeekWithNewAsset() {
        String currentDate = getDateFormatter("dd-MMM-yyyy").format(Instant.now());
        Asset createAsset = getTestAsset("week-cached-asset");
        assetService.persistAsset(createAsset, user);

        Map<String, Map<String, GraphData>> firstData = statisticsDataService.getCachedGraphData(GraphView.WEEK);

        assertThat(firstData).containsKey("incremental");
        assertThat(firstData.get("incremental")).containsKey(currentDate);
        int prevInstituteSpecimens = firstData.get("incremental").get(currentDate).getInstitutes().get("institution_1");
//        assertThat(firstData.get("incremental").get(currentDate).getInstitutes().get("institution_1")).isEqualTo(2);

        Asset newCreateAsset = getTestAsset("new-week-cached-asset");
        assetService.persistAsset(newCreateAsset, user);

        // adds a new asset with 2 specimens
        Map<String, Map<String, GraphData>> secondData = statisticsDataService.getCachedGraphData(GraphView.WEEK);

        assertThat(secondData).containsKey("incremental");
        assertThat(secondData.get("incremental")).containsKey(currentDate);
        assertThat(secondData.get("incremental").get(currentDate).getInstitutes().get("institution_1")).isEqualTo(prevInstituteSpecimens + 2);
    }

    @Test
    public void calculcateCachedYearWithNewAsset() {
        String currentDate = getDateFormatter("MMM yyyy").format(Instant.now());
        Asset createAsset = getTestAsset("year-cached-asset");
        assetService.persistAsset(createAsset,user);

        Map<String, Map<String, GraphData>> firstData = statisticsDataService.getCachedGraphData(GraphView.YEAR);

        assertThat(firstData).containsKey("incremental");
        assertThat(firstData.get("incremental")).containsKey(currentDate);
        int prevInstituteSpecimensIncr = firstData.get("incremental").get(currentDate).getInstitutes().get("institution_1");
        assertThat(firstData).containsKey("exponential");
        assertThat(firstData.get("exponential")).containsKey(currentDate);
        int prevInstituteSpecimensExpon = firstData.get("exponential").get(currentDate).getInstitutes().get("institution_1");

        Asset newCreateAsset = getTestAsset("new-year-asset");
        assetService.persistAsset(newCreateAsset, user);

        // adds a new asset with 2 specimens
        Map<String, Map<String, GraphData>> secondData = statisticsDataService.getCachedGraphData(GraphView.YEAR);

        assertThat(secondData).containsKey("incremental");
        assertThat(secondData.get("incremental")).containsKey(currentDate);
        assertThat(secondData.get("incremental").get(currentDate).getInstitutes().get("institution_1")).isEqualTo(prevInstituteSpecimensIncr + 2);
        assertThat(secondData).containsKey("exponential");
        assertThat(secondData.get("exponential")).containsKey(currentDate);
        assertThat(secondData.get("exponential").get(currentDate).getInstitutes().get("institution_1")).isEqualTo(prevInstituteSpecimensExpon + 2);
    }

    public Asset getTestAsset(String guid) {
        Asset asset = new Asset();
        asset.asset_locked = false;
        asset.digitizer = "Karl-Børge";
        asset.guid = guid;
        asset.funding = "Hundredetusindvis af dollars";
        asset.asset_taken_date = Instant.now();
        asset.subject = "Folder";
        asset.file_formats = Arrays.asList(FileFormat.JPEG);
        asset.payload_type = "nuclear";
        asset.updateUser = "Basviola";
        asset.specimen_barcodes = Arrays.asList("createAsset-sp-1", "createAsset-sp-2");
        asset.pipeline = "i1_p1";
        asset.workstation = "i1_w1";
        asset.tags.put("Tag1", "value1");
        asset.tags.put("Tag2", "value2");
        asset.institution = "institution_1";
        asset.collection = "i1_c1";
        asset.pid = "pid-createAsset";
        asset.status = AssetStatus.BEING_PROCESSED;
        return asset;
    }

    public DateTimeFormatter getDateFormatter(String pattern) { // need this as the pattern varies >.>
        return new DateTimeFormatterBuilder() // default day and hour as the pattern is only month and year
                .appendPattern(pattern)
                .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .toFormatter(Locale.ENGLISH)
                .withZone(ZoneId.of("UTC"));
    }
}
