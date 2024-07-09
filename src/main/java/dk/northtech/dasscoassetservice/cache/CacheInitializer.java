package dk.northtech.dasscoassetservice.cache;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.repositories.*;
import org.springframework.context.ApplicationListener;
import jakarta.inject.Inject;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CacheInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private InstitutionCache institutionCache;
    private InstitutionRepository institutionRepository;
    private CollectionCache collectionCache;
    private CollectionRepository collectionRepository;
    private PipelineCache pipelineCache;
    private PipelineRepository pipelineRepository;
    private WorkstationCache workstationCache;
    private WorkstationRepository workstationRepository;
    private DigitiserRepository digitiserRepository;
    private DigitiserCache digitiserCache;
    private boolean initialized = false;

    @Inject
    public CacheInitializer(InstitutionCache institutionCache, InstitutionRepository institutionRepository,
                            CollectionCache collectionCache, CollectionRepository collectionRepository,
                            PipelineCache pipelineCache, PipelineRepository pipelineRepository,
                            WorkstationCache workstationCache, WorkstationRepository workstationRepository,
                            DigitiserRepository digitiserRepository, DigitiserCache digitiserCache){
        this.institutionCache = institutionCache;
        this.institutionRepository = institutionRepository;
        this.collectionCache = collectionCache;
        this.collectionRepository = collectionRepository;
        this.pipelineCache = pipelineCache;
        this.pipelineRepository = pipelineRepository;
        this.workstationCache = workstationCache;
        this.workstationRepository = workstationRepository;
        this.digitiserRepository = digitiserRepository;
        this.digitiserCache = digitiserCache;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event){
        if (!initialized){
            List<Institution> institutionList = institutionRepository.listInstitutions();
            if (!institutionList.isEmpty()){
                for (Institution institution : institutionList){
                    institutionCache.putInstitutionInCache(institution.name(), institution);
                    List<Collection> collectionList = collectionRepository.listCollections(institution);
                    if (!collectionList.isEmpty()){
                        for (Collection collection : collectionList){
                            this.collectionCache.putCollectionInCache(institution.name(), collection.name(), collection);
                        }
                    }
                    List<Pipeline> pipelineList = pipelineRepository.listPipelines(institution);
                    if (!pipelineList.isEmpty()){
                        for (Pipeline pipeline : pipelineList){
                            this.pipelineCache.putPipelineInCache(pipeline);
                        }
                    }
                    List<Workstation> workstationList = workstationRepository.listWorkStations(institution);
                    if (!workstationList.isEmpty()){
                        for (Workstation workstation : workstationList){
                            this.workstationCache.putWorkstationInCache(workstation);
                        }
                    }
                    List<Digitiser> digitiserList = digitiserRepository.listDigitisers();
                    if (!digitiserList.isEmpty()){
                        for (Digitiser digitiser : digitiserList){
                            this.digitiserCache.putDigitiserInCache(digitiser);
                        }
                    }
                }
            }
            initialized = true;
        }
    }
}
