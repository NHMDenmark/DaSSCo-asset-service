package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.google.common.truth.Truth.assertThat;
import static dk.northtech.dasscoassetservice.domain.GraphType.exponential;
import static dk.northtech.dasscoassetservice.domain.GraphType.incremental;

@Disabled("Disabled for now")
public class StatisticsDataServiceTest extends AbstractIntegrationTest {
    User user = new User();

//    @Test
//    public void temp() {
////        Instant startDate = Instant.now().minus(15, ChronoUnit.DAYS);
//        Instant startDate = ZonedDateTime.now(ZoneOffset.UTC).minusYears(1).toInstant();
//        System.out.println(startDate.toEpochMilli());
//        System.out.println(Instant.now().toEpochMilli());
//
//        Map<String, GraphData> data = statisticsDataServicev2.generateIncrDataV2(startDate, Instant.now(), GraphView.YEAR);
//        System.out.println(data);
////        Map<String, GraphData> accumulated = statisticsDataServicev2.accumulatedData(data);
////        System.out.println(accumulated);
////        Map<String, GraphData> data = statisticsDataService.generateIncrData(startDate, Instant.now(), DateTimeFormatter.ofPattern("MM-yyyy").withZone(ZoneId.of("UTC")), GraphView.YEAR);
//
////        Map<String, GraphData> total = statisticsDataService.generateTotalIncrData(data, DateTimeFormatter.ofPattern("MMM-yyyy").withZone(ZoneId.of("UTC")));
//        Map<String, GraphData> total = statisticsDataServicev2.totalValues(data);
//        System.out.println(total);
//
////        statisticsDataService.testOfNewSQL(startDate, Instant.now());
//    }

//    @Test
//    public void calculcateWeek() {
//        Asset createAsset = getTestAsset("week-asset", "institution_1", 0);
//        assetService.persistAsset(createAsset, user,11);
//
//        Instant startDate = ZonedDateTime.now(ZoneOffset.UTC).minusWeeks(1).toInstant();
//        DateTimeFormatter nf = DateTimeFormatter.ofPattern("dd-MMM-yyyy").withZone(ZoneId.of("UTC"));
//        String currentDate = nf.format(Instant.now());
//        long duration = ChronoUnit.DAYS.between(startDate, Instant.now()) + 1; // plus 1 as it doesn't count the first date as "between"
//
//        Map<GraphType, Map<String, GraphData>> finalData = statisticsDataServicev2.getCachedGraphData(GraphView.WEEK);
//
//        assertThat(finalData).containsKey(incremental);
//        assertThat(finalData.get(incremental)).isNotEmpty();
//        assertThat(finalData.get(incremental)).containsKey(currentDate);
//        assertThat(finalData.get(incremental).get(currentDate).getInstitutes().get("institution_1")).isEqualTo(2);
//        assertThat(finalData.get(incremental).size()).isEqualTo(duration);
//
//        Map.Entry<String, GraphData> firstEntry = finalData.get(incremental).entrySet().iterator().next();
//        assertThat(firstEntry.getKey()).isEqualTo(nf.format(startDate));
//    }
//
//    @Test
//    @Disabled // disabled for now as I can't find a way atm to have it "start over" and not count the assets from the other tests. aka "expected 2 but was 14"
//    public void calculcateMonth() {
//        Asset createAsset = getTestAsset("month-asset", "institution_1", 1);
//        assetService.persistAsset(createAsset, user,11);
//
//        Instant startDate = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(1).toInstant();
//        String currentDate = getDateFormatter("dd-MMM-yyyy").format(Instant.now());
//        long duration = ChronoUnit.DAYS.between(startDate, Instant.now()) + 1; // plus 1 as it doesn't count the first date as "between"
//
//        Map<GraphType, Map<String, GraphData>> finalData = statisticsDataServicev2.getCachedGraphData(GraphView.MONTH);
//
//        assertThat(finalData).containsKey(incremental);
//        assertThat(finalData.get(incremental)).isNotEmpty();
//        assertThat(finalData.get(incremental)).containsKey(currentDate);
//        assertThat(finalData.get(incremental).get(currentDate).getInstitutes().get("institution_1")).isEqualTo(2);
//        assertThat(finalData.get(incremental).size()).isEqualTo(duration);
//
//        Map.Entry<String, GraphData> firstEntry = finalData.get(incremental).entrySet().iterator().next();
//        assertThat(firstEntry.getKey()).isEqualTo(getDateFormatter("dd-MMM-yyyy").format(startDate));
//    }
//
//    @Test
//    @Disabled
//    public void calculcateCachedWeekWithNewAsset() {
//        DateTimeFormatter nf = DateTimeFormatter.ofPattern("dd-MMM-yyyy").withZone(ZoneId.of("UTC"));
//        String currentDate = nf.format(Instant.now());
//        Asset createAsset = getTestAsset("week-cached-asset", "institution_1", 2);
//            assetService.persistAsset(createAsset, user,11);
//
//        Map<GraphType, Map<String, GraphData>> firstData = statisticsDataServicev2.getCachedGraphData(GraphView.WEEK);
//
//        assertThat(firstData).containsKey(incremental);
//        assertThat(firstData.get(incremental)).containsKey(currentDate);
//        int prevInstituteSpecimens = firstData.get(incremental).get(currentDate).getInstitutes().get("institution_1");
//
//        Asset newCreateAsset = getTestAsset("new-week-cached-asset", "institution_1", 3);
//
//        assetService.persistAsset(newCreateAsset, user,11);
//
//        // adds a new asset with 2 specimens
//        Map<GraphType, Map<String, GraphData>> secondData = statisticsDataServicev2.getCachedGraphData(GraphView.WEEK);
//
//        assertThat(secondData).containsKey(incremental);
//        assertThat(secondData.get(incremental)).containsKey(currentDate);
//        assertThat(secondData.get(incremental).get(currentDate).getInstitutes().get("institution_1")).isEqualTo(prevInstituteSpecimens + 2);
//    }
//
//    @Test
//    @Disabled
//    public void calculcateCachedYearWithNewAsset() {
//        DateTimeFormatter nf = DateTimeFormatter.ofPattern("MMM yyyy").withZone(ZoneId.of("UTC"));
//        String currentDate = nf.format(Instant.now());
//
//        Asset createAsset = getTestAsset("year-cached-asset", "institution_1", 4);
//        assetService.persistAsset(createAsset,user,11);
//
//        Map<GraphType, Map<String, GraphData>> firstData = statisticsDataServicev2.getCachedGraphData(GraphView.YEAR);
//
//        assertThat(firstData).containsKey(incremental);
//        assertThat(firstData.get(incremental)).containsKey(currentDate);
//        int prevInstituteSpecimensIncr = firstData.get(incremental).get(currentDate).getInstitutes().get("Institutes");
//        assertThat(firstData).containsKey(exponential);
//        assertThat(firstData.get(exponential)).containsKey(currentDate);
//        int prevInstituteSpecimensExpon = firstData.get(exponential).get(currentDate).getInstitutes().get("institution_1");
//
//        Asset newCreateAsset = getTestAsset("new-year-cached-asset", "institution_1", 5);
//        assetService.persistAsset(newCreateAsset, user,11);
//
//        // adds a new asset with 2 specimens
//        Map<GraphType, Map<String, GraphData>> secondData = statisticsDataServicev2.getCachedGraphData(GraphView.YEAR);
//
//        assertThat(secondData).containsKey(incremental);
//        assertThat(secondData.get(incremental)).containsKey(currentDate);
//        assertThat(secondData.get(incremental).get(currentDate).getInstitutes().get("Institutes")).isEqualTo(prevInstituteSpecimensIncr + 2);
//        assertThat(secondData).containsKey(exponential);
//        assertThat(secondData.get(exponential)).containsKey(currentDate);
//        assertThat(secondData.get(exponential).get(currentDate).getInstitutes().get("institution_1")).isEqualTo(prevInstituteSpecimensExpon + 2);
//    }
//
//    @Test
//    @Disabled
//    public void calucalateYearTotal() {
//        DateTimeFormatter nf = DateTimeFormatter.ofPattern("MMM yyyy").withZone(ZoneId.of("UTC"));
//        String currentDate = nf.format(Instant.now());
//
//        Asset createAsset = getTestAsset("year-total-asset", "institution_1", 6);
//        assetService.persistAsset(createAsset, user,11);
//        Map<GraphType, Map<String, GraphData>> beforeData = this.statisticsDataServicev2.getCachedGraphData(GraphView.YEAR);
//        Integer instSumBefore = beforeData.get(incremental).get(currentDate).getInstitutes().values().stream().reduce(0, Integer::sum);
//
//        Asset createAssetNew = getTestAsset("new-year-total-asset", "institution_2", "i2_p1", 7, "i2_c1");
//        assetService.persistAsset(createAssetNew, user,11);
//        Map<GraphType, Map<String, GraphData>> dataAfter = this.statisticsDataServicev2.getCachedGraphData(GraphView.YEAR);
//        Integer instSumAfter = dataAfter.get(incremental).get(currentDate).getInstitutes().values().stream().reduce(0, Integer::sum);
//
//        assertThat(dataAfter).containsKey(incremental);
//        assertThat(dataAfter.get(incremental)).containsKey(currentDate);
//        assertThat(dataAfter).containsKey(exponential);
//        assertThat(dataAfter.get(exponential)).containsKey(currentDate);
//        assertThat(instSumBefore).isLessThan(instSumAfter);
//    }

    // the id is to keep the specimens unique, otherwise we'll get errors in the amount when persisting the assets
//    public Asset getTestAsset(String guid, String instituteName, int id) {
//        return getTestAsset(guid,instituteName, "i1_p1", id, "i1_c1");
//    }

//    public Asset getTestAsset(String guid, String instituteName, String pipelineName, int id, String collectionName) {
//        Asset asset = new Asset();
//        asset.asset_locked = false;
//        asset.digitiser = "Karl-Børge";
//        asset.asset_guid = guid;
//        asset.funding = Arrays.asList("Hundredetusindvis af dollars");
//        asset.date_asset_taken = Instant.now();
//        asset.asset_subject = "Folder";
//        asset.file_formats = Arrays.asList("JPEG");
//        asset.payload_type = "nuclear";
//        asset.updateUser = "Basviola";
//        asset.assetSpecimens = Arrays.asList(new Specimen(instituteName, "i1_c1", "creatAsset-sp-" + id, "spid" + id, new HashSet<>(Set.of("slide")), "slide")
//                , new Specimen(instituteName, "i1_c1", "creatAsset-sp-0" + id, "spid0" + id, new HashSet<>(Set.of("slide")), "slide"));
//        asset.pipeline = pipelineName;
//        asset.workstation = "i1_w1";
//        asset.tags.put("Tag1", "value1");
//        asset.tags.put("Tag2", "value2");
//        asset.institution = instituteName;
//        asset.collection = collectionName;
//        asset.asset_pid = "pid-createAsset";
//        asset.status = "BEING_PROCESSED";
//        return asset;
//    }

    public DateTimeFormatter getDateFormatter(String pattern) { // need this as the pattern varies >.>
        return new DateTimeFormatterBuilder() // default day and hour as the pattern is only month and year
                .appendPattern(pattern)
                .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .toFormatter(Locale.ENGLISH)
                .withZone(ZoneId.of("UTC"));
    }
}
