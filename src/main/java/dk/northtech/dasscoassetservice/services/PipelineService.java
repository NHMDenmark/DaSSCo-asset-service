package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Pipeline;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.Pipeline;
import dk.northtech.dasscoassetservice.repositories.PipelineRepository;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PipelineService {
    private final InstitutionService institutionService;
    private final PipelineRepository pipelineRepository;


    @Inject
    public PipelineService(InstitutionService institutionService, PipelineRepository pipelineRepository) {
        this.institutionService = institutionService;
        this.pipelineRepository = pipelineRepository;
    }

    public Pipeline persistPipeline(Pipeline pipeline) {
        Optional<Institution> ifExists = institutionService.getIfExists(pipeline.institution());
        if(ifExists.isEmpty()){
            throw new IllegalArgumentException("Institute doesnt exist");
        }
        Optional<Pipeline> piplOpt = findPipeline(pipeline.name());
        if(piplOpt.isPresent()){
            Pipeline pipeline1 = piplOpt.get();
            throw new IllegalArgumentException("A pipeline with name ["+pipeline1.name()+"] already exists within institution ["+pipeline1.institution()+"]");

        }
        pipelineRepository.persistPipeline(pipeline);
        return pipeline;

    }

    public List<Pipeline> listPipelines(Institution institution) {
        return pipelineRepository.listPipelines(institution);
    }

    public Optional<Pipeline> findPipeline(String pipelineName) {
        return this.pipelineRepository.findPipeline(pipelineName);
    }
}
