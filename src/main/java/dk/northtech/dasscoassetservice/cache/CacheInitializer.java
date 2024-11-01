package dk.northtech.dasscoassetservice.cache;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.repositories.*;
import dk.northtech.dasscoassetservice.webapi.v1.StatisticsDataApi;
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

    private final InstitutionCache institutionCache;
    private final CollectionCache collectionCache;
    private final PipelineCache pipelineCache;
    private final PipelineRepository pipelineRepository;
    private final WorkstationCache workstationCache;
    private final WorkstationRepository workstationRepository;
    private final DigitiserRepository digitiserRepository;
    private final DigitiserCache digitiserCache;
    private final SubjectCache subjectCache;
    private final PayloadTypeCache payloadTypeCache;
    private final PreparationTypeCache preparationTypeCache;
    private final StatusCache statusCache;
    private final RestrictedAccessCache restrictedAccessCache;
    private boolean initialized = false;
    private static final Logger logger = LoggerFactory.getLogger(CacheInitializer.class);
    private final Jdbi jdbi;

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
                    institutionCache.putInstitutionInCacheIfAbsent(institution.name(), institution);
                    List<Collection> collectionList = jdbi.withHandle(h -> {
                        CollectionRepository collectionRepository = h.attach(CollectionRepository.class);
                        return collectionRepository.listCollections(institution);
                    });
                    if (!collectionList.isEmpty()){
                        for (Collection collection : collectionList){
                            this.collectionCache.putCollectionInCacheIfAbsent(institution.name(), collection.name(), collection);
                        }
                    }
                    List<Pipeline> pipelineList = pipelineRepository.listPipelines(institution);
                    if (!pipelineList.isEmpty()){
                        for (Pipeline pipeline : pipelineList){
                            this.pipelineCache.putPipelineInCacheIfAbsent(pipeline);
                        }
                    }
                    List<Workstation> workstationList = workstationRepository.listWorkStations(institution);
                    if (!workstationList.isEmpty()){
                        for (Workstation workstation : workstationList){
                            this.workstationCache.putWorkstationInCacheIfAbsent(workstation);
                        }
                    }
                }
                List<Digitiser> digitiserList = digitiserRepository.listDigitisers();
                if (!digitiserList.isEmpty()){
                    for (Digitiser digitiser : digitiserList){
                        this.digitiserCache.putDigitiserInCacheIfAbsent(digitiser);
                    }
                }
                List<String> subjectList = jdbi.withHandle(handle -> {
                    AssetRepository assetRepository = handle.attach(AssetRepository.class);
                    return assetRepository.listSubjects();
                });
                if (!subjectList.isEmpty()){
                    for (String subject : subjectList){
                        this.subjectCache.putSubjectsInCacheIfAbsent(subject);
                    }
                }
                List<String> payloadTypeList = jdbi.withHandle(handle -> {
                    AssetRepository assetRepository = handle.attach(AssetRepository.class);
                    return assetRepository.listPayloadTypes();
                });
                if (!payloadTypeList.isEmpty()){
                    for (String payloadType : payloadTypeList){
                        this.payloadTypeCache.putPayloadTypesInCacheIfAbsent(payloadType);
                    }
                }
                List<String> preparationTypeList = jdbi.withHandle(handle -> {
                    SpecimenRepository specimenRepository = handle.attach(SpecimenRepository.class);
                    return specimenRepository.listPreparationTypes();
                });
                if (!preparationTypeList.isEmpty()){
                    for (String preparationType : preparationTypeList){
                        this.preparationTypeCache.putPreparationTypesInCacheIfAbsent(preparationType);
                    }
                }
                List<AssetStatus> statusList = jdbi.withHandle(handle -> {
                   AssetRepository assetRepository = handle.attach(AssetRepository.class);
                   return assetRepository.listStatus();
                });
                if (!statusList.isEmpty()){
                    for(AssetStatus status : statusList){
                        this.statusCache.putStatusInCacheIfAbsent(status);
                    }
                }
                List<String> restrictedAccessList = jdbi.withHandle(handle -> {
                   AssetRepository assetRepository = handle.attach(AssetRepository.class);
                   return assetRepository.listRestrictedAccess();
                });
                if (!restrictedAccessList.isEmpty()){
                    for (String internalRole : restrictedAccessList){
                        this.restrictedAccessCache.putRestrictedAccessInCacheIfAbsent(internalRole);
                    }
                }
            }

            logger.info("CacheInitializer initialized with the following caches:");
            logger.info("InstitutionCache: {}", institutionCache != null ? institutionCache.getInstitutionMap(): "Not present");
            logger.info("CollectionCache: {}", collectionCache != null ? collectionCache.getCollectionMap() : "Not present");
            logger.info("PipelineCache: {}", pipelineCache != null ? pipelineCache.getPipelineMap() : "Not present");
            logger.info("DigitiserCache: {}", digitiserCache != null ? digitiserCache.getDigitiserMap() : "Not present");
            logger.info("SubjectCache: {}", subjectCache != null ? subjectCache.getSubjectMap() : "Not present");
            logger.info("PayloadTypeCache: {}", payloadTypeCache != null ? payloadTypeCache.getPayloadTypeMap() : "Not present");
            logger.info("PreparationTypeCache: {}", preparationTypeCache != null ? preparationTypeCache.getPreparationTypeMap() : "Not present");
            logger.info("StatusCache: {}", statusCache != null ? statusCache.getStatusMap() : "Not present");
            logger.info("RestrictedAccessCache: {}", restrictedAccessCache != null ? restrictedAccessCache.getRestrictedAccessMap() : "Not present");
            logger.info("WorkstationCache: {}", workstationCache != null ? workstationCache.getWorkstationMap() : "Not present");

            initialized = true;
        }
    }
}
