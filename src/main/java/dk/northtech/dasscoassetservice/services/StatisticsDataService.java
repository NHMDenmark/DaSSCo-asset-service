package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.StatisticsData;
import dk.northtech.dasscoassetservice.repositories.StatisticsDataRepository;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatisticsDataService {
    private final StatisticsDataRepository statisticsDataRepository;

    @Inject
    public StatisticsDataService(StatisticsDataRepository statisticsDataRepository) {
        this.statisticsDataRepository = statisticsDataRepository;
    }

    public List<StatisticsData> getGraphData() {
        return this.statisticsDataRepository.getGraphData();
    }

}
