package dk.northtech.dasscoassetservice.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.GraphData;
import dk.northtech.dasscoassetservice.domain.GraphView;
import dk.northtech.dasscoassetservice.domain.StatisticsData;
import dk.northtech.dasscoassetservice.repositories.StatisticsDataRepository;
import jakarta.inject.Inject;
import joptsimple.internal.Strings;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class StatisticsDataService {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsDataService.class);
    private final StatisticsDataRepository statisticsDataRepository;

    LoadingCache<GraphView, Map<String, Map<String, GraphData>>> cachedGraphData = CacheBuilder.newBuilder()
            .expireAfterAccess(24, TimeUnit.HOURS)
            .build(
                    new CacheLoader<GraphView, Map<String, Map<String, GraphData>>>() {
                        public Map<String, Map<String, GraphData>> load(GraphView key) {
                            // {incremental (pr day data): data, exponential (continually adding pr day): data}
                            Map<String, Map<String, GraphData>> finalData = new ListOrderedMap<>();
                            Map<String, GraphData> incrData;

                            if (key.equals(GraphView.WEEK)) {
                                logger.info("Generating, and caching, daily data for the past week.");
                                Instant startDate = ZonedDateTime.now(ZoneOffset.UTC).minusWeeks(1).toInstant();
                                incrData = generateIncrData(startDate, Instant.now(), getDateFormatter("dd-MMM-yyyy"), GraphView.WEEK);
                                finalData.put("incremental", incrData);
                            } else if (key.equals(GraphView.MONTH)) {
                                logger.info("Generating, and caching, daily data for the past month.");
                                Instant startDate = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(1).toInstant();
                                incrData = generateIncrData(startDate, Instant.now(), getDateFormatter("dd-MMM-yyyy"), GraphView.MONTH);
                                finalData.put("incremental", incrData);
                            } else if (key.equals(GraphView.YEAR)) {
                                logger.info("Generating, and caching, monthly data for the past year.");
                                Instant startDate = ZonedDateTime.now(ZoneOffset.UTC).minusYears(1).toInstant();
                                incrData = generateIncrData(startDate, Instant.now(), getDateFormatter("MMM yyyy"), GraphView.YEAR);
                                finalData = generateExponData(incrData, getDateFormatter("MMM yyyy"));
                            }

                            return finalData;
                        }
                    });

    @Inject
    public StatisticsDataService(StatisticsDataRepository statisticsDataRepository) {
        this.statisticsDataRepository = statisticsDataRepository;
    }

    public Map<String, Map<String, GraphData>> getCachedGraphData(GraphView timeFrame) {
        try {
            return cachedGraphData.get(timeFrame);
        } catch (ExecutionException e) {
            logger.warn("An error occurred when loading the graph cache {}", e.getMessage());
            throw new RuntimeException("An error occurred when loading the graph cache {}", e);
        }
    }

    public List<StatisticsData> getGraphData(long timeFrame) {
        return this.statisticsDataRepository.getGraphData(timeFrame, Instant.now().toEpochMilli());
    }

    public Map<String, GraphData> generateIncrData(Instant startDate, Instant endDate, DateTimeFormatter dateTimeFormatter, GraphView timeFrame) {
        List<StatisticsData> statisticsData = this.statisticsDataRepository.getGraphData(startDate.toEpochMilli(), endDate.toEpochMilli());
        Map<String, GraphData> incrData = new HashMap<>();

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

        addRemainingDates(startDate, endDate, dateTimeFormatter, incrData, timeFrame);
        return sortMapOnDateKeys(incrData, dateTimeFormatter);
    }

    public Map<String, Map<String, GraphData>> generateExponData(Map<String, GraphData> originalData, DateTimeFormatter dateFormatter) {
        Map<String, Map<String, GraphData>> finalData = new HashMap<>(); // linechart: data, barchart: data
        Gson gson = new Gson(); // not a huge fan of this, but is the only way I can see - for now - to deep clone the map.
        String jsonString = gson.toJson(originalData);
        Type type = new TypeToken<HashMap<String, GraphData>>(){}.getType();
        HashMap<String, GraphData> deepClonedData = gson.fromJson(jsonString, type);

        // I know, but for some reason the deepcloning messes up the order pft
        ListOrderedMap<String, GraphData> exponData = sortMapOnDateKeys(deepClonedData, dateFormatter);

        // then adds the values to the next map entry to get the exponential values
        MapIterator<String, GraphData> it = exponData.mapIterator();
        while (it.hasNext()) {
            String key = it.next();
            GraphData currvalue = it.getValue();
            if (!Strings.isNullOrEmpty(exponData.nextKey(key))) { // if there's a next
                GraphData nextVal = deepClonedData.get(exponData.nextKey(key));

                // gets all institute names of current data, runs through, adds their value to the next data object
                currvalue.getInstitutes().keySet().forEach(instituteName -> nextVal.addInstituteAmts(instituteName, currvalue.getInstitutes().get(instituteName)));
                currvalue.getPipelines().keySet().forEach(pipelineName -> nextVal.addPipelineAmts(pipelineName, currvalue.getPipelines().get(pipelineName)));
                currvalue.getWorkstations().keySet().forEach(workstationName -> nextVal.addWorkstationAmts(workstationName, currvalue.getWorkstations().get(workstationName)));
            }
        }
        finalData.put("incremental", originalData);
        finalData.put("exponential", exponData);

        return finalData;
    }

    public void updateData(Map<String, Integer> existing, String key, Integer specimens) {
        if (existing.containsKey(key)) {
            int existingVal = existing.get(key);
            existing.put(key, existingVal + specimens);
        } else {
            existing.put(key, specimens);
        }
    }

    public void addRemainingDates(Instant startDate, Instant endDate, DateTimeFormatter dateTimeFormatter, Map<String, GraphData> data, GraphView timeFrame) {
        List<String> dates = new ArrayList<>(); // dates = labels aka. x-axis
        ChronoUnit unit = null;

        if (timeFrame.equals(GraphView.WEEK) || timeFrame.equals(GraphView.MONTH)) {
            unit = ChronoUnit.DAYS;
        } else if (timeFrame.equals(GraphView.YEAR) || timeFrame.equals(GraphView.EXPONENTIAL)) { // we want the labels as jan, feb, march, etc. if year
            unit = ChronoUnit.MONTHS;
        }

        long difference = unit.between(LocalDateTime.ofInstant(startDate, dateTimeFormatter.getZone()), LocalDateTime.ofInstant(endDate, dateTimeFormatter.getZone()));

        for (long i = difference; i >= 0; i--) { // adds all labels/dates between the dates
            dates.add(dateTimeFormatter.format(LocalDateTime.ofInstant(endDate, dateTimeFormatter.getZone()).minus(i, unit)));
        }

        // Adds the dates from start- to end date, on which there are no data (for the sake of the js graph)
        dates.forEach(date -> { if (!data.containsKey(date)) data.put(date, new GraphData(new HashMap<>(), new HashMap<>(), new HashMap<>())); });
    }

    public ListOrderedMap<String, GraphData> sortMapOnDateKeys(Map<String, GraphData> unsortedMap, DateTimeFormatter dateFormatter) {
        ListOrderedMap<String, GraphData> sortedMap = unsortedMap.entrySet()
                .stream()
                .sorted(Comparator.comparing(e -> dateFormatter.parse(e.getKey(), Instant::from)))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, ListOrderedMap::new));
        return sortedMap;
    }

    public DateTimeFormatter getDateFormatter(String pattern) { // need this as the pattern varies >.>
        return new DateTimeFormatterBuilder() // default day and hour as the pattern is only month and year
                .appendPattern(pattern)
                .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .toFormatter(Locale.ENGLISH)
                .withZone(ZoneId.of("UTC"));
    }

    public void addAssetToCache(Asset asset) {
        try {
            if (cachedGraphData.asMap().containsKey(GraphView.WEEK)) {
                updateCache(asset, cachedGraphData.get(GraphView.WEEK), "incremental", getDateFormatter("dd-MMM-yyyy"));
            }
            if (cachedGraphData.asMap().containsKey(GraphView.MONTH)) {
                updateCache(asset, cachedGraphData.get(GraphView.MONTH), "incremental", getDateFormatter("dd-MMM-yyyy"));
            }
            if (cachedGraphData.asMap().containsKey(GraphView.YEAR)) {
                updateCache(asset, cachedGraphData.get(GraphView.YEAR), "incremental", getDateFormatter("MMM yyyy"));
                updateCache(asset, cachedGraphData.get(GraphView.YEAR), "exponential", getDateFormatter("MMM yyyy"));
            }
        } catch (ExecutionException e) {
            logger.warn("An error occurred when loading the graph cache {}", e.getMessage());
            throw new RuntimeException("An error occurred when loading the graph cache {}", e);
        }
    }

    public void updateCache(Asset asset, Map<String, Map<String, GraphData>> cachedFullData, String key, DateTimeFormatter dtf) {
        Map<String, GraphData> cachedData = cachedFullData.get(key);
        String createdDate = dtf.format(asset.created_date);

        if (cachedData.containsKey(createdDate)) {
            logger.info("New asset with {} specimens is being added.", asset.specimen_barcodes.size());
            cachedData.get(createdDate).addInstituteAmts(asset.institution, asset.specimen_barcodes.size());
            cachedData.get(createdDate).addWorkstationAmts(asset.workstation, asset.specimen_barcodes.size());
            cachedData.get(createdDate).addPipelineAmts(asset.pipeline, asset.specimen_barcodes.size());
        } else {
            logger.info("Cached data does not contain today's date {}, and will be added.", createdDate);
            cachedData.put(createdDate, new GraphData(
                    new HashMap<>() {{put(asset.institution, asset.specimen_barcodes.size());}},
                    new HashMap<>() {{put(asset.pipeline, asset.specimen_barcodes.size());}},
                    new HashMap<>() {{put(asset.workstation, asset.specimen_barcodes.size());}}
            ));
            cachedFullData.put(key, sortMapOnDateKeys(cachedData, getDateFormatter("dd-MMM-yyyy")));
        }
    }
}
