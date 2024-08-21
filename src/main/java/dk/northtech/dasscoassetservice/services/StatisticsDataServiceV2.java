package dk.northtech.dasscoassetservice.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dk.northtech.dasscoassetservice.domain.*;
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
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static dk.northtech.dasscoassetservice.domain.GraphType.exponential;
import static dk.northtech.dasscoassetservice.domain.GraphType.incremental;

@Service
public class StatisticsDataServiceV2 {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsDataServiceV2.class);
    private final StatisticsDataRepository statisticsDataRepository;
    private final InternalStatusService internalStatusService;

    LoadingCache<GraphView, Map<GraphType, Map<String, GraphData>>> cachedGraphData = CacheBuilder.newBuilder()
            .expireAfterAccess(24, TimeUnit.HOURS)
            .build(
                    new CacheLoader<GraphView, Map<GraphType, Map<String, GraphData>>>() {
                        public Map<GraphType, Map<String, GraphData>> load(GraphView key) {
                            // {incremental (pr day data): data, exponential (continually adding pr day): data}
                            Map<GraphType, Map<String, GraphData>> finalData = new ListOrderedMap<>();

                            if (key.equals(GraphView.WEEK)) {
                                logger.info("Generating and caching daily data for the past week.");
                                Instant startDate = ZonedDateTime.now(ZoneOffset.UTC).minusWeeks(1).toInstant();
                                Map<String, GraphData> incrData = generateIncrDataV2(startDate, Instant.now(), GraphView.WEEK);

                                finalData.put(incremental, incrData);
                            } else if (key.equals(GraphView.MONTH)) {
                                logger.info("Generating and caching daily data for the past month.");
                                Instant startDate = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(1).toInstant();
                                Map<String, GraphData> incrData = generateIncrDataV2(startDate, Instant.now(), GraphView.MONTH);

                                finalData.put(incremental, incrData);
                            } else if (key.equals(GraphView.YEAR)) {
                                logger.info("Generating and caching monthly data for the past year.");
                                Instant startDate = ZonedDateTime.now(ZoneOffset.UTC).minusYears(1).toInstant();
                                finalData = getYearlyData(startDate, Instant.now(), GraphView.EXPONENTIAL); // exponential as we wanna cache both kinds of graphs.
                            }
                            return finalData;
                        }
                    });

    @Inject
    public StatisticsDataServiceV2(StatisticsDataRepository statisticsDataRepository, InternalStatusService internalStatusService) {
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

    public Map<GraphType, Map<String, GraphData>> getYearlyData(Instant startDate, Instant endDate, GraphView graphView) {
        Map<GraphType, Map<String, GraphData>> finalData = new ListOrderedMap<>();

        Map<String, GraphData> incrData = generateIncrDataV2(startDate, endDate, graphView);
        if (!incrData.isEmpty()) {
            Map<String, GraphData> totalValues = totalValues(incrData);
            finalData.put(incremental, totalValues);

            if (graphView.equals(GraphView.EXPONENTIAL)) {
                // OBS: this is commented atm, as I spoke to Pip and we realised that it didn't make sense to show the data like this.
                    // It's not deleted in case we will need it again at a later date.
//                Map<String, GraphData> exponData = accumulatedData(incrData);
//                finalData.put(exponential, exponData);
                finalData.put(exponential, incrData);
            }
        }

        return finalData;
    }

    public Map<String, GraphData> generateIncrDataV2(Instant startDate, Instant endDate, GraphView graphView) {
        List<StatisticsData> stats = this.statisticsDataRepository.getGraphData(startDate.toEpochMilli(), endDate.toEpochMilli());

        Map<String, GraphData> graphDataMap = new LinkedHashMap<>();
        Instant currentDate = startDate;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy").withZone(ZoneId.of("UTC"));
        if (graphView.equals(GraphView.YEAR) || graphView.equals(GraphView.EXPONENTIAL)) { // to be shown pr month instead of pr day
            formatter = DateTimeFormatter.ofPattern("MMM yyyy").withZone(ZoneId.of("UTC"));
        }

        while (!currentDate.isAfter(endDate)) {
            String dateKey = formatter.format(currentDate);
            graphDataMap.put(dateKey, new GraphData());

            if (graphView.equals(GraphView.YEAR) || graphView.equals(GraphView.EXPONENTIAL)) { // to be shown pr month instead of pr day
                LocalDate currentDateLocal = currentDate.atZone(ZoneId.of("UTC")).toLocalDate();
                currentDateLocal = currentDateLocal.plusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
                currentDate = currentDateLocal.atStartOfDay(ZoneId.of("UTC")).toInstant();
            } else {
                currentDate = currentDate.plus(1, ChronoUnit.DAYS);
            }
        }

        for (StatisticsData stat : stats) {
            String dateKey = formatter.format(Instant.ofEpochMilli(stat.createdDate()));
            GraphData graphData = graphDataMap.getOrDefault(dateKey, new GraphData());

            graphData.getInstitutes().merge(stat.instituteName(), stat.specimens(), Integer::sum);
            graphData.getPipelines().merge(stat.pipelineName(), stat.specimens(), Integer::sum);
            graphData.getWorkstations().merge(stat.workstationName(), stat.specimens(), Integer::sum);

            graphDataMap.put(dateKey, graphData);
        }
        return graphDataMap;
    }

    public Map<String, GraphData> accumulatedData(Map<String, GraphData> graphDataMap) {
        Map<String, GraphData> sortedData = new LinkedHashMap<>(graphDataMap);

        GraphData previousData = new GraphData();

        for (Map.Entry<String, GraphData> entry : sortedData.entrySet()) {
            String month = entry.getKey();
            GraphData currentData = entry.getValue();

            accumulateValues(previousData, currentData);
            sortedData.put(month, currentData);

            previousData = currentData;
        }
        return sortedData;
    }

    private void accumulateValues(GraphData previous, GraphData current) {
        current.getInstitutes().forEach((key, value) ->
                current.getInstitutes().put(key, value + previous.getInstitutes().getOrDefault(key, 0))
        );

        current.getPipelines().forEach((key, value) ->
                current.getPipelines().put(key, value + previous.getPipelines().getOrDefault(key, 0))
        );

        current.getWorkstations().forEach((key, value) ->
                current.getWorkstations().put(key, value + previous.getWorkstations().getOrDefault(key, 0))
        );
    }

    public Map<String, GraphData> totalValues(Map<String, GraphData> graphDataMap) {
        Map<String, GraphData> summarizedMap = new LinkedHashMap<>();

        for (Map.Entry<String, GraphData> entry : graphDataMap.entrySet()) {
            String dateKey = entry.getKey();
            GraphData originalData = entry.getValue();

            // Sum up the counts for institutions, pipelines, and workstations
            int totalInstitutions = originalData.getInstitutes().values().stream().mapToInt(Integer::intValue).sum();
            int totalPipelines = originalData.getPipelines().values().stream().mapToInt(Integer::intValue).sum();
            int totalWorkstations = originalData.getWorkstations().values().stream().mapToInt(Integer::intValue).sum();

            // Create a new GraphData object with these summed values
            Map<String, Integer> summarizedInstitutions = Collections.singletonMap("Institutes", totalInstitutions);
            Map<String, Integer> summarizedPipelines = Collections.singletonMap("Pipelines", totalPipelines);
            Map<String, Integer> summarizedWorkstations = Collections.singletonMap("Workstations", totalWorkstations);

            GraphData summarizedData = new GraphData();
            summarizedData.setInstitutes(summarizedInstitutions);
            summarizedData.setPipelines(summarizedPipelines);
            summarizedData.setWorkstations(summarizedWorkstations);

            // Put the new GraphData into the summarized map
            summarizedMap.put(dateKey, summarizedData);
        }

        return summarizedMap;
    }
}
