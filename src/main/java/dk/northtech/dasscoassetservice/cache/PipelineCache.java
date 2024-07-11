package dk.northtech.dasscoassetservice.cache;

import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Pipeline;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PipelineCache {
    private Map<String, Pipeline> pipelineMap = new HashMap<>();

    public Map<String, Pipeline> getPipelineCache() {
        return pipelineMap;
    }

    public void setPipelineCache(Map<String, Pipeline> pipelineMap) {
        this.pipelineMap = pipelineMap;
    }

    public void putPipelineInCache(Pipeline pipeline){
        this.pipelineMap.put(pipeline.name(), pipeline);
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
