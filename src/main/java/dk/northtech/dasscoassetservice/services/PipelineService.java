package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.cache.InstitutionCache;
import dk.northtech.dasscoassetservice.cache.PipelineCache;
import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.CollectionRoleRestriction;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.Pipeline;
import dk.northtech.dasscoassetservice.repositories.CollectionRepository;
import dk.northtech.dasscoassetservice.repositories.PipelineRepository;
import dk.northtech.dasscoassetservice.repositories.RoleRepository;
import jakarta.inject.Inject;
import joptsimple.internal.Strings;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PipelineService {

    private final ConcurrentHashMap<String, ConcurrentHashMap<String,Pipeline>> pipelineMap = new ConcurrentHashMap<>();
    private Jdbi jdbi;
    private boolean initialised = false;
    private InstitutionService institutionService;
    private static final Logger logger = LoggerFactory.getLogger(PipelineService.class);
    @Inject
    public PipelineService(Jdbi jdbi, InstitutionService institutionService) {
        this.jdbi = jdbi;
        this.institutionService = institutionService;
    }


    public Pipeline persistPipeline(Pipeline pipeline, String institutionName) {
        if(!this.initialised) {
            this.initPipelines();
        }
        if (Objects.isNull(pipeline)){
            throw new IllegalArgumentException("POST request requires a body");
        }

        if (Strings.isNullOrEmpty(pipeline.name())){
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        if (institutionService.getIfExists(institutionName).isEmpty()){
            throw new IllegalArgumentException("Institution doesnt exist");
        }

        boolean exists = this.pipelineMap.containsKey(institutionName) && this.pipelineMap.get(institutionName).get(pipeline.name()) != null;
        if (exists){
            throw new IllegalArgumentException("A pipeline with name ["+pipeline.name()+"] already exists within institution ["+institutionName+"]");
        }

        Pipeline pipe = new Pipeline(pipeline.name(), institutionName);
        jdbi.withHandle(handle -> {
            PipelineRepository attach = handle.attach(PipelineRepository.class);
            Pipeline persistedPipeline = attach.persistPipeline(pipe);
            // make sure institution exists
            this.pipelineMap.computeIfAbsent(pipe.institution() , x -> new ConcurrentHashMap<>()).put(persistedPipeline.name(),persistedPipeline);
            return persistedPipeline;
        });

        return pipeline;
    }

    public List<Pipeline> listPipelines(Institution institution) {
        if(!this.initialised) {
            this.initPipelines();
        }
        if (institutionService.getIfExists(institution.name()).isEmpty()){
            throw new IllegalArgumentException("Institute does not exist");
        }
        if(pipelineMap.containsKey(institution.name())){
            return new ArrayList<>(pipelineMap.get(institution.name()).values());
        }
        return new ArrayList<>();

    }

//    public Optional<Pipeline> findPipeline(String pipelineName) {
//        if(!this.initialised) {
//            this.initPipelines();
//        }
//        return this.pipelineRepository.findPipeline(pipelineName);
//    }

    public Optional<Pipeline> findPipelineByInstitutionAndName(String pipelineName, String institutionName) {
        if(!this.initialised) {
            this.initPipelines();
        }

        if(pipelineName != null && institutionName != null && this.pipelineMap.containsKey(institutionName)) {
            Pipeline pipeline = pipelineMap.get(institutionName).get(pipelineName);
            return pipeline != null ? Optional.of(pipeline) : Optional.empty();
        }
        return Optional.empty();
    }

    public void initPipelines() {
        synchronized (this) {
            if (!this.initialised) {
                jdbi.withHandle(h -> {
                    this.institutionService.listInstitutions().forEach(i -> this.pipelineMap.put(i.name(), new ConcurrentHashMap<>()));
                    PipelineRepository pipelineRepository = h.attach(PipelineRepository.class);
                    List<Pipeline> pipelines = pipelineRepository.listPipelines();
                    for (Pipeline pipeline: pipelines) {
                        this.pipelineMap.get(pipeline.institution()).put(pipeline.name(), pipeline);
                    }
                    logger.info("Loaded {} collections", pipelineMap.size());
                    return h;
                });
                this.initialised = true;
            }
        }
    }
}
