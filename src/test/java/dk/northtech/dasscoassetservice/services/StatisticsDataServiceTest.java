package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
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
import static dk.northtech.dasscoassetservice.domain.GraphType.exponential;
import static dk.northtech.dasscoassetservice.domain.GraphType.incremental;

public class StatisticsDataServiceTest extends AbstractIntegrationTest {
    User user = new User();
    @Test
    public void calculcateWeek() {
        Asset createAsset = getTestAsset("week-asset", "institution_1");
        assetService.persistAsset(createAsset, user);

        Instant startDate = ZonedDateTime.now(ZoneOffset.UTC).minusWeeks(1).toInstant();
        String currentDate = getDateFormatter("dd-MMM-yyyy").format(Instant.now());
        long duration = ChronoUnit.DAYS.between(startDate, Instant.now()) + 1; // plus 1 as it doesn't count the first date as "between"

        Map<GraphType, Map<String, GraphData>> finalData = statisticsDataService.getCachedGraphData(GraphView.WEEK);

        assertThat(finalData).containsKey(incremental);
        assertThat(finalData.get(incremental)).isNotEmpty();
        assertThat(finalData.get(incremental)).containsKey(currentDate);
        assertThat(finalData.get(incremental).get(currentDate).getInstitutes().get("institution_1")).isEqualTo(2);
        assertThat(finalData.get(incremental).size()).isEqualTo(duration);

        Map.Entry<String, GraphData> firstEntry = finalData.get(incremental).entrySet().iterator().next();
        assertThat(firstEntry.getKey()).isEqualTo(getDateFormatter("dd-MMM-yyyy").format(startDate));
    }

    @Test
    public void calculcateMonth() {
        Asset createAsset = getTestAsset("month-asset", "institution_1");
        assetService.persistAsset(createAsset, user);

        Instant startDate = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(1).toInstant();
        String currentDate = getDateFormatter("dd-MMM-yyyy").format(Instant.now());
        long duration = ChronoUnit.DAYS.between(startDate, Instant.now()) + 1; // plus 1 as it doesn't count the first date as "between"

        Map<GraphType, Map<String, GraphData>> finalData = statisticsDataService.getCachedGraphData(GraphView.MONTH);

        assertThat(finalData).containsKey(incremental);
        assertThat(finalData.get(incremental)).isNotEmpty();
        assertThat(finalData.get(incremental)).containsKey(currentDate);
        assertThat(finalData.get(incremental).get(currentDate).getInstitutes().get("institution_1")).isEqualTo(2);
        assertThat(finalData.get(incremental).size()).isEqualTo(duration);

        Map.Entry<String, GraphData> firstEntry = finalData.get(incremental).entrySet().iterator().next();
        assertThat(firstEntry.getKey()).isEqualTo(getDateFormatter("dd-MMM-yyyy").format(startDate));
    }

    @Test
    public void calculcateCachedWeekWithNewAsset() {
        String currentDate = getDateFormatter("dd-MMM-yyyy").format(Instant.now());
        Asset createAsset = getTestAsset("week-cached-asset", "institution_1");
            assetService.persistAsset(createAsset, user);


        Map<GraphType, Map<String, GraphData>> firstData = statisticsDataService.getCachedGraphData(GraphView.WEEK);

        assertThat(firstData).containsKey(incremental);
        assertThat(firstData.get(incremental)).containsKey(currentDate);
        int prevInstituteSpecimens = firstData.get(incremental).get(currentDate).getInstitutes().get("institution_1");

        Asset newCreateAsset = getTestAsset("new-week-cached-asset", "institution_1");
        assetService.persistAsset(newCreateAsset, user);

        // adds a new asset with 2 specimens
        Map<GraphType, Map<String, GraphData>> secondData = statisticsDataService.getCachedGraphData(GraphView.WEEK);

        assertThat(secondData).containsKey(incremental);
        assertThat(secondData.get(incremental)).containsKey(currentDate);
        assertThat(secondData.get(incremental).get(currentDate).getInstitutes().get("institution_1")).isEqualTo(prevInstituteSpecimens + 2);
    }

    @Test
    public void calculcateCachedYearWithNewAsset() {
        String currentDate = getDateFormatter("MMM yyyy").format(Instant.now());

        Asset createAsset = getTestAsset("year-cached-asset", "institution_1");
        assetService.persistAsset(createAsset,user);

        Map<GraphType, Map<String, GraphData>> firstData = statisticsDataService.getCachedGraphData(GraphView.YEAR);

        assertThat(firstData).containsKey(incremental);
        assertThat(firstData.get(incremental)).containsKey(currentDate);
        int prevInstituteSpecimensIncr = firstData.get(incremental).get(currentDate).getInstitutes().get("Institutes");
        assertThat(firstData).containsKey(exponential);
        assertThat(firstData.get(exponential)).containsKey(currentDate);
        int prevInstituteSpecimensExpon = firstData.get(exponential).get(currentDate).getInstitutes().get("institution_1");

        Asset newCreateAsset = getTestAsset("new-year-cached-asset", "institution_1");
        assetService.persistAsset(newCreateAsset, user);

        // adds a new asset with 2 specimens
        Map<GraphType, Map<String, GraphData>> secondData = statisticsDataService.getCachedGraphData(GraphView.YEAR);

        assertThat(secondData).containsKey(incremental);
        assertThat(secondData.get(incremental)).containsKey(currentDate);
        assertThat(secondData.get(incremental).get(currentDate).getInstitutes().get("Institutes")).isEqualTo(prevInstituteSpecimensIncr + 2);
        assertThat(secondData).containsKey(exponential);
        assertThat(secondData.get(exponential)).containsKey(currentDate);
        assertThat(secondData.get(exponential).get(currentDate).getInstitutes().get("institution_1")).isEqualTo(prevInstituteSpecimensExpon + 2);
    }

    @Test
    public void calucalateYearTotal() {
        String currentDate = getDateFormatter("MMM yyyy").format(Instant.now());

        Asset createAsset = getTestAsset("year-total-asset", "institution_1");
        assetService.persistAsset(createAsset, user);
        Map<GraphType, Map<String, GraphData>> beforeData = this.statisticsDataService.getCachedGraphData(GraphView.YEAR);
        Integer instSumBefore = beforeData.get(incremental).get(currentDate).getInstitutes().values().stream().reduce(0, Integer::sum);

        Asset createAssetNew = getTestAsset("new-year-total-asset", "institution_2", "i2_p1");
        assetService.persistAsset(createAssetNew, user);
        Map<GraphType, Map<String, GraphData>> dataAfter = this.statisticsDataService.getCachedGraphData(GraphView.YEAR);
        Integer instSumAfter = dataAfter.get(incremental).get(currentDate).getInstitutes().values().stream().reduce(0, Integer::sum);
        System.out.println(dataAfter);

        assertThat(dataAfter).containsKey(incremental);
        assertThat(dataAfter.get(incremental)).containsKey(currentDate);
        assertThat(dataAfter).containsKey(exponential);
        assertThat(dataAfter.get(exponential)).containsKey(currentDate);
        assertThat(instSumBefore).isLessThan(instSumAfter);
    }

    public Asset getTestAsset(String guid, String instituteName) {
        return getTestAsset(guid,instituteName, "i1_p1");
    }
    public Asset getTestAsset(String guid, String instituteName, String pipelineName) {
        Asset asset = new Asset();
        asset.asset_locked = false;
        asset.digitiser = "Karl-Børge";
        asset.asset_guid = guid;
        asset.funding = "Hundredetusindvis af dollars";
        asset.date_asset_taken = Instant.now();
        asset.subject = "Folder";
        asset.file_formats = Arrays.asList(FileFormat.JPEG);
        asset.payload_type = "nuclear";
        asset.updateUser = "Basviola";
        asset.specimens = Arrays.asList(new Specimen(instituteName, "i1_c1", "creatAsset-sp-1", "spid1", "slide"), new Specimen(instituteName, "i1_c1", "creatAsset-sp-2", "spid1", "slide"));
        asset.pipeline = pipelineName;
        asset.workstation = "i1_w1";
        asset.tags.put("Tag1", "value1");
        asset.tags.put("Tag2", "value2");
        asset.institution = instituteName;
        asset.collection = "i1_c1";
        asset.asset_pid = "pid-createAsset";
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
