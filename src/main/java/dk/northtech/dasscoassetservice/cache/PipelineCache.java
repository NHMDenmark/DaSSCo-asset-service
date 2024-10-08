package dk.northtech.dasscoassetservice.cache;

import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Pipeline;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class PipelineCache {
    private final ConcurrentHashMap<String, Pipeline> pipelineMap = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, Pipeline> getPipelineMap() {
        return pipelineMap;
    }

    public void putPipelineInCacheIfAbsent(Pipeline pipeline){
        this.pipelineMap.putIfAbsent(pipeline.name(), pipeline);
    }

    public List<Pipeline> getPipelines(String institution){
        return pipelineMap.values().stream()
                .filter(pipeline -> institution.equals(pipeline.institution()))
                .collect(Collectors.toList());
    }

    public Pipeline pipelineExists(String pipelineName){
        return pipelineMap.get(pipelineName);
    }
}
