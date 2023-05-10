package dk.northtech.dasscoassetservice.services;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dk.northtech.dasscoassetservice.domain.GraphData;
import dk.northtech.dasscoassetservice.domain.StatisticsData;
import dk.northtech.dasscoassetservice.repositories.StatisticsDataRepository;
import dk.northtech.dasscoassetservice.webapi.v1.StatisticsDataApi;
import jakarta.inject.Inject;
import joptsimple.internal.Strings;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

//    public List<StatisticsData> getGraphData(long timeFrame) {
//        return this.statisticsDataRepository.getGraphData(timeFrame);
//    }

    public Map<String, GraphData> generateIncrData(long timeFrame, DateTimeFormatter dateTimeFormatter) {
        List<StatisticsData> statisticsData = this.statisticsDataRepository.getGraphData(timeFrame);
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
