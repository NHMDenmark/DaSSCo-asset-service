package dk.northtech.dasscoassetservice.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatisticsDataService {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsDataService.class);
    private final StatisticsDataRepository statisticsDataRepository;

    @Inject
    public StatisticsDataService(StatisticsDataRepository statisticsDataRepository) {
        this.statisticsDataRepository = statisticsDataRepository;
    }

    public List<StatisticsData> getGraphData(long timeFrame) {
        return this.statisticsDataRepository.getGraphData(timeFrame);
    }

    public Map<String, GraphData> generateIncrData(Instant startDate, Instant endDate, DateTimeFormatter dateTimeFormatter, GraphView timeFrame) {
        List<StatisticsData> statisticsData = this.statisticsDataRepository.getGraphData(startDate.toEpochMilli());
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

//        return incrData;
    }

    public List<Map<String, GraphData>> generateExponData(Map<String, GraphData> originalData, DateTimeFormatter dateFormatter) {
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
            GraphData value = it.getValue();
            if (!Strings.isNullOrEmpty(exponData.nextKey(key))) { // if there's a next
                GraphData nextVal = deepClonedData.get(exponData.nextKey(key));
                if (!value.getInstitutes().isEmpty()) {
                    value.getInstitutes().keySet().forEach(instituteName -> nextVal.addInstituteAmts(instituteName, value.getInstitutes().get(instituteName)));
                }
                if (!value.getPipelines().isEmpty()) {
                    value.getPipelines().keySet().forEach(pipelineName -> nextVal.addPipelineAmts(pipelineName, value.getPipelines().get(pipelineName)));
                }
                if (!value.getWorkstations().isEmpty()) {
                    value.getWorkstations().keySet().forEach(workstationName -> nextVal.addWorkstationAmts(workstationName, value.getWorkstations().get(workstationName)));
                }
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

    public void addRemainingDates(Instant startDate, Instant endDate, DateTimeFormatter dateTimeFormatter, Map<String, GraphData> data, GraphView timeFrame) {
        List<String> dates = new ArrayList<>(); // dates = labels aka. x-axis
        ChronoUnit unit = null;

        if (timeFrame.equals(GraphView.WEEK) || timeFrame.equals(GraphView.MONTH)) {
            unit = ChronoUnit.DAYS;
        } else if (timeFrame.equals(GraphView.YEAR)) { // we want the labels as jan, feb, march, etc. if year
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
}
