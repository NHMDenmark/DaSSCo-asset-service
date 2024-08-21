package dk.northtech.dasscoassetservice.cache;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.repositories.*;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.CreateSqlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import jakarta.inject.Inject;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CacheInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private InstitutionCache institutionCache;
    private CollectionCache collectionCache;
    private PipelineCache pipelineCache;
    private PipelineRepository pipelineRepository;
    private WorkstationCache workstationCache;
    private WorkstationRepository workstationRepository;
    private DigitiserRepository digitiserRepository;
    private DigitiserCache digitiserCache;
    private SubjectCache subjectCache;
    private PayloadTypeCache payloadTypeCache;
    private PreparationTypeCache preparationTypeCache;
    private StatusCache statusCache;
    private RestrictedAccessCache restrictedAccessCache;
    private boolean initialized = false;
    Jdbi jdbi;
    private static final Logger logger = LoggerFactory.getLogger(CacheInitializer.class);

    @Inject
    public CacheInitializer(InstitutionCache institutionCache,
                            CollectionCache collectionCache,
                            PipelineCache pipelineCache, PipelineRepository pipelineRepository,
                            WorkstationCache workstationCache, WorkstationRepository workstationRepository,
                            DigitiserRepository digitiserRepository, DigitiserCache digitiserCache,
                            SubjectCache subjectCache,
                            PayloadTypeCache payloadTypeCache,
                            PreparationTypeCache preparationTypeCache,
                            StatusCache statusCache,
                            RestrictedAccessCache restrictedAccessCache,
                            Jdbi jdbi){
        this.institutionCache = institutionCache;
        this.collectionCache = collectionCache;
        this.pipelineCache = pipelineCache;
        this.pipelineRepository = pipelineRepository;
        this.workstationCache = workstationCache;
        this.workstationRepository = workstationRepository;
        this.digitiserRepository = digitiserRepository;
        this.digitiserCache = digitiserCache;
        this.subjectCache = subjectCache;
        this.payloadTypeCache = payloadTypeCache;
        this.preparationTypeCache = preparationTypeCache;
        this.statusCache = statusCache;
        this.restrictedAccessCache = restrictedAccessCache;
        this.jdbi = jdbi;

        logInitialization();
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event){
        if (!initialized){
            List<Institution> institutionList = jdbi.withHandle(h -> {
                InstitutionRepository institutionRepository = h.attach(InstitutionRepository.class);
                return institutionRepository.listInstitutions();
            });
            if (!institutionList.isEmpty()){
                for (Institution institution : institutionList) {
                    institutionCache.putInstitutionInCache(institution.name(), institution);
                    List<Collection> collectionList = jdbi.withHandle(h -> {
                        CollectionRepository collectionRepository = h.attach(CollectionRepository.class);
                        return collectionRepository.listCollections(institution);
                    });
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

                logger.info("Institution Cache " + institutionCache.getInstitutionMap());
                logger.info("Collection Cache " + collectionCache.getCollectionMap());
                logger.info("Workstation Cache " + workstationCache.getWorkstationMap());
                logger.info("Pipeline Cache " + pipelineCache.getPipelineCache());

                List<Digitiser> digitiserList = digitiserRepository.listDigitisers();
                if (!digitiserList.isEmpty()){
                    for (Digitiser digitiser : digitiserList){
                        this.digitiserCache.putDigitiserInCache(digitiser);
                    }
                }

                logger.info("Digitiser List: " + digitiserList);
                logger.info("Digitiser Cache: " + digitiserCache.getDigitiserMap());

                List<String> subjectList = jdbi.withHandle(handle -> {
                    AssetRepository assetRepository = handle.attach(AssetRepository.class);
                    return assetRepository.listSubjects();
                });
                if (!subjectList.isEmpty()){
                    for (String subject : subjectList){
                        this.subjectCache.putSubjectsInCache(subject);
                    }
                }

                logger.info("Subject List: " + subjectList);
                logger.info("Subject Cache: " + subjectCache.getSubjectMap());

                List<String> payloadTypeList = jdbi.withHandle(handle -> {
                    AssetRepository assetRepository = handle.attach(AssetRepository.class);
                    return assetRepository.listPayloadTypes();
                });
                if (!payloadTypeList.isEmpty()){
                    for (String payloadType : payloadTypeList){
                        this.payloadTypeCache.putPayloadTypesInCache(payloadType);
                    }
                }
                List<String> preparationTypeList = jdbi.withHandle(handle -> {
                    SpecimenRepository specimenRepository = handle.attach(SpecimenRepository.class);
                    return specimenRepository.listPreparationTypes();
                });
                if (!preparationTypeList.isEmpty()){
                    for (String preparationType : preparationTypeList){
                        this.preparationTypeCache.putPreparationTypesInCache(preparationType);
                    }
                }
                List<AssetStatus> statusList = jdbi.withHandle(handle -> {
                   AssetRepository assetRepository = handle.attach(AssetRepository.class);
                   return assetRepository.listStatus();
                });
                if (!statusList.isEmpty()){
                    for(AssetStatus status : statusList){
                        this.statusCache.putStatusInCache(status);
                    }
                }
                List<String> restrictedAccessList = jdbi.withHandle(handle -> {
                   AssetRepository assetRepository = handle.attach(AssetRepository.class);
                   return assetRepository.listRestrictedAccess();
                });
                if (!restrictedAccessList.isEmpty()){
                    for (String internalRole : restrictedAccessList){
                        this.restrictedAccessCache.putRestrictedAccessInCache(internalRole);
                    }
                }
            }
            initialized = true;
        }
    }

    private void logInitialization() {
        logger.info("CacheInitializer initialized with the following caches:");
        logger.info("InstitutionCache: {}", institutionCache != null ? "Present" : "Not present");
        logger.info("CollectionCache: {}", collectionCache != null ? "Present" : "Not present");
        logger.info("PipelineCache: {}", pipelineCache != null ? "Present" : "Not present");
        logger.info("DigitiserCache: {}", digitiserCache != null ? "Present" : "Not present");
        logger.info("SubjectCache: {}", subjectCache != null ? "Present" : "Not present");
        logger.info("PayloadTypeCache: {}", payloadTypeCache != null ? "Present" : "Not present");
        logger.info("PreparationTypeCache: {}", preparationTypeCache != null ? "Present" : "Not present");
        logger.info("StatusCache: {}", statusCache != null ? "Present" : "Not present");
        logger.info("RestrictedAccessCache: {}", restrictedAccessCache != null ? "Present" : "Not present");
        logger.info("WorkstationCache: {}", restrictedAccessCache != null ? "Present" : "Not present");
    }
}
