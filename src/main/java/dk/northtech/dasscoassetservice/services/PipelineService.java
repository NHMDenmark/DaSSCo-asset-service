package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.cache.InstitutionCache;
import dk.northtech.dasscoassetservice.cache.PipelineCache;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.Pipeline;
import dk.northtech.dasscoassetservice.repositories.PipelineRepository;
import jakarta.inject.Inject;
import joptsimple.internal.Strings;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class PipelineService {
    private final PipelineRepository pipelineRepository;
    private InstitutionCache institutionCache;
    private PipelineCache pipelineCache;


    @Inject
    public PipelineService(PipelineRepository pipelineRepository,
                           InstitutionCache institutionCache, PipelineCache pipelineCache) {
        this.pipelineRepository = pipelineRepository;
        this.institutionCache = institutionCache;
        this.pipelineCache = pipelineCache;
    }

    public Pipeline persistPipeline(Pipeline pipeline, String institutionName) {

        if (Objects.isNull(pipeline)){
            throw new IllegalArgumentException("POST request requires a body");
        }

        if (Strings.isNullOrEmpty(pipeline.name())){
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        if (!institutionCache.institutionExists(institutionName)){
            throw new IllegalArgumentException("Institute doesnt exist");
        }

        Pipeline exists = pipelineCache.pipelineExists(pipeline.name());
        if (exists != null){
            throw new IllegalArgumentException("A pipeline with name ["+exists.name()+"] already exists within institution ["+exists.institution()+"]");

        }

        Pipeline pipe = new Pipeline(pipeline.name(), institutionName);

        pipelineRepository.persistPipeline(pipe);
        pipelineCache.putPipelineInCache(pipe);

        return pipeline;
    }

    public List<Pipeline> listPipelines(Institution institution) {
        System.out.println("Institution:");
        System.out.println(institution);
        if (!institutionCache.institutionExists(institution.name())){
            throw new IllegalArgumentException("Institute does not exist");
        }
        return pipelineCache.getPipelines(institution.name());
    }

    public Optional<Pipeline> findPipeline(String pipelineName) {
        return this.pipelineRepository.findPipeline(pipelineName);
    }

    public Optional<Pipeline> findPipelineByInstitutionAndName(String pipelineName, String institutionName) {
        return this.pipelineRepository.findPipelineByInstitutionAndName(pipelineName, institutionName);
    }
}
