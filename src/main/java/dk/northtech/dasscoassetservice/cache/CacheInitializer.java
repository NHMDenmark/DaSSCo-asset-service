package dk.northtech.dasscoassetservice.cache;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.repositories.*;
import org.jdbi.v3.sqlobject.CreateSqlObject;
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
    private SubjectRepository subjectRepository;
    private SubjectCache subjectCache;
    private PayloadTypeRepository payloadTypeRepository;
    private PayloadTypeCache payloadTypeCache;
    private PreparationTypeCache preparationTypeCache;
    private PreparationTypeRepository preparationTypeRepository;
    private StatusCache statusCache;
    private StatusRepository statusRepository;
    private boolean initialized = false;

    @Inject
    public CacheInitializer(InstitutionCache institutionCache, InstitutionRepository institutionRepository,
                            CollectionCache collectionCache, CollectionRepository collectionRepository,
                            PipelineCache pipelineCache, PipelineRepository pipelineRepository,
                            WorkstationCache workstationCache, WorkstationRepository workstationRepository,
                            DigitiserRepository digitiserRepository, DigitiserCache digitiserCache,
                            SubjectRepository subjectRepository, SubjectCache subjectCache,
                            PayloadTypeRepository payloadTypeRepository, PayloadTypeCache payloadTypeCache,
                            PreparationTypeCache preparationTypeCache, PreparationTypeRepository preparationTypeRepository,
                            StatusRepository statusRepository, StatusCache statusCache){
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
        this.subjectRepository = subjectRepository;
        this.subjectCache = subjectCache;
        this.payloadTypeRepository = payloadTypeRepository;
        this.payloadTypeCache = payloadTypeCache;
        this.preparationTypeCache = preparationTypeCache;
        this.preparationTypeRepository = preparationTypeRepository;
        this.statusRepository = statusRepository;
        this.statusCache = statusCache;
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
                }
                List<Digitiser> digitiserList = digitiserRepository.listDigitisers();
                if (!digitiserList.isEmpty()){
                    for (Digitiser digitiser : digitiserList){
                        this.digitiserCache.putDigitiserInCache(digitiser);
                    }
                }
                List<String> subjectList = subjectRepository.listSubjects();
                if (!subjectList.isEmpty()){
                    for (String subject : subjectList){
                        this.subjectCache.putSubjectsInCache(subject);
                    }
                }
                List<String> payloadTypeList = payloadTypeRepository.listPayloadTypes();
                if (!payloadTypeList.isEmpty()){
                    for (String payloadType : payloadTypeList){
                        this.payloadTypeCache.putPayloadTypesInCache(payloadType);
                    }
                }

                List<String> preparationTypeList = preparationTypeRepository.listPreparationTypes();
                if (!preparationTypeList.isEmpty()){
                    for (String preparationType : preparationTypeList){
                        this.preparationTypeCache.putPreparationTypesInCache(preparationType);
                    }
                }
                List<AssetStatus> statusList = statusRepository.listStatus();
                if (!statusList.isEmpty()){
                    for(AssetStatus status : statusList){
                        this.statusCache.putStatusInCache(status);
                    }
                }
            }
            initialized = true;
        }
    }
}
