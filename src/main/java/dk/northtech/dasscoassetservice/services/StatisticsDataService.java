package dk.northtech.dasscoassetservice.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.repositories.StatisticsDataRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
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

import static dk.northtech.dasscoassetservice.domain.GraphType.exponential;
import static dk.northtech.dasscoassetservice.domain.GraphType.incremental;

@Service
public class StatisticsDataService {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsDataService.class);
    private final StatisticsDataRepository statisticsDataRepository;
    private final InternalStatusService internalStatusService;

    LoadingCache<GraphView, Map<GraphType, Map<String, GraphData>>> cachedGraphData = CacheBuilder.newBuilder()
            .expireAfterAccess(24, TimeUnit.HOURS)
            .build(
                    new CacheLoader<GraphView, Map<GraphType, Map<String, GraphData>>>() {
                        public Map<GraphType, Map<String, GraphData>> load(GraphView key) {
                            // {incremental (pr day data): data, exponential (continually adding pr day): data}
                            Map<GraphType, Map<String, GraphData>> finalData = new ListOrderedMap<>();
                            Map<String, GraphData> incrData;

                            if (key.equals(GraphView.WEEK)) {
                                logger.info("Generating and caching daily data for the past week.");
                                Instant startDate = ZonedDateTime.now(ZoneOffset.UTC).minusWeeks(1).toInstant();
                                incrData = generateIncrData(startDate, Instant.now(), getDateFormatter("dd-MMM-yyyy"), GraphView.WEEK);

                                finalData.put(incremental, incrData);
                            } else if (key.equals(GraphView.MONTH)) {
                                logger.info("Generating and caching daily data for the past month.");
                                Instant startDate = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(1).toInstant();
                                incrData = generateIncrData(startDate, Instant.now(), getDateFormatter("dd-MMM-yyyy"), GraphView.MONTH);

                                finalData.put(incremental, incrData);
                            } else if (key.equals(GraphView.YEAR)) {
                                logger.info("Generating and caching monthly data for the past year.");
                                DateTimeFormatter dtf = getDateFormatter("MMM yyyy");
                                Instant startDate = ZonedDateTime.now(ZoneOffset.UTC).minusYears(1).toInstant();
                                                                incrData = generateIncrData(startDate, Instant.now(), getDateFormatter("MMM yyyy"), GraphView.YEAR);
                                Map<String, GraphData> totalData = generateTotalIncrData(incrData, dtf);
                                Map<String, GraphData> exponData = generateExponData(incrData, dtf);

                                finalData.put(incremental, totalData);
                                finalData.put(exponential, exponData);
                            }

                            return finalData;
                        }
                    });

    @Inject
    public StatisticsDataService(StatisticsDataRepository statisticsDataRepository, InternalStatusService internalStatusService) {
        this.statisticsDataRepository = statisticsDataRepository;
        this.internalStatusService = internalStatusService;
    }

    public Map<GraphType, Map<String, GraphData>> getCachedGraphData(GraphView timeFrame) {
        try {
            return cachedGraphData.get(timeFrame);
        } catch (ExecutionException e) {
            logger.warn("An error occurred when loading the graph cache {}", e.getMessage());
            throw new RuntimeException("An error occurred when loading the graph cache {}", e);
        }
    }

    public void refreshCachedData() {
        refreshGraphDataCache();
        if (internalStatusService.cachedInternalStatus != null && !internalStatusService.cachedInternalStatus.asMap().isEmpty()) {
            for (InternalStatusTimeFrame view : InternalStatusTimeFrame.values()) {
                internalStatusService.cachedInternalStatus.refresh(view); // refresh only "refreshes" next time .get() is called
            }
        }
    }

    public void refreshGraphDataCache() {
        if (cachedGraphData != null && !cachedGraphData.asMap().isEmpty()) {
            for (GraphView view : GraphView.values()) {
                if (cachedGraphData.asMap().containsKey(view)) {
                    cachedGraphData.refresh(view); // refresh only "refreshes" next time .get() is called
                }
            }
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

            updateOnKey(incrData, dateString,
                    data.instituteName(), data.specimens(),
                    data.workstationName(), data.specimens(),
                    data.pipelineName(), data.specimens());

        });

        addRemainingDates(startDate, endDate, dateTimeFormatter, incrData, timeFrame);
        return sortMapOnDateKeys(incrData, dateTimeFormatter);
    }

    public Map<String, GraphData> generateTotalIncrData(Map<String, GraphData> incrData, DateTimeFormatter dateTimeFormatter) {
        Map<String, GraphData> totalData = new HashMap<>();;

        incrData.forEach((dateKey, value) -> {
            // gets all the values from all institues on each date and adds them
            Integer instituteSum = value.getInstitutes().values().stream().reduce(0, Integer::sum);
            Integer pipelineSum = value.getPipelines().values().stream().reduce(0, Integer::sum);
            Integer workstationSum = value.getWorkstations().values().stream().reduce(0, Integer::sum);

            updateOnKey(totalData, dateKey,
                    "Institutes", instituteSum,
                    "Workstations", workstationSum,
                    "Pipelines", pipelineSum);
        });

        return sortMapOnDateKeys(totalData, dateTimeFormatter);
    }

    public void updateOnKey(Map<String, GraphData> dataMap, String key,
                            String institute, Integer instituteAmount,
                            String workstation, Integer workstationAmount,
                            String pipeline, Integer pipelineAmount) {
        if (!dataMap.containsKey(key)) {
            dataMap.put(key, new GraphData(
                    new HashMap<>() {{put(institute, instituteAmount);}},
                    new HashMap<>() {{put(pipeline, pipelineAmount);}},
                    new HashMap<>() {{put(workstation, workstationAmount);}}
            ));
        } else {
            updateData(dataMap.get(key).getInstitutes(), institute, instituteAmount);
            updateData(dataMap.get(key).getPipelines(), pipeline, pipelineAmount);
            updateData(dataMap.get(key).getWorkstations(), workstation, workstationAmount);
        }
    }

    public Map<String, GraphData> generateExponData(Map<String, GraphData> originalData, DateTimeFormatter dateFormatter) {
//    Map<String, Map<String, GraphData>> finalData = new HashMap<>(); // linechart: data, barchart: data
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

        return exponData;
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
}
