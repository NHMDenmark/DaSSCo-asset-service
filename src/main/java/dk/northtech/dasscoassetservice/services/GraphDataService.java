package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.GraphData;
import dk.northtech.dasscoassetservice.repositories.CollectionRepository;
import dk.northtech.dasscoassetservice.repositories.GraphDataRepository;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GraphDataService {
    private final GraphDataRepository graphDataRepository;

    @Inject
    public GraphDataService(GraphDataRepository graphDataRepository) {
        this.graphDataRepository = graphDataRepository;
    }

    public List<GraphData> getGraphData() {
        return this.graphDataRepository.getGraphData();
    }

}
