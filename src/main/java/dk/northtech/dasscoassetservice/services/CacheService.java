package dk.northtech.dasscoassetservice.services;

import com.google.inject.Inject;
import dk.northtech.dasscoassetservice.cache.*;
import dk.northtech.dasscoassetservice.domain.AssetStatus;
import dk.northtech.dasscoassetservice.domain.Digitiser;
import dk.northtech.dasscoassetservice.domain.InternalRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CacheService {
    private final CollectionCache collectionCache;
    private final DigitiserCache digitiserCache;
    private final InstitutionCache institutionCache;
    private final PayloadTypeCache payloadTypeCache;
    private final PipelineCache pipelineCache;
    private final ExtendableEnumService extendableEnumService;
    private final PreparationTypeCache preparationTypeCache;
    private final SubjectCache subjectCache;
    private final WorkstationCache workstationCache;
    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    private final CollectionService collectionService;
    private final RoleService roleService;

    @Inject
    @Lazy
    public CacheService(CollectionCache collectionCache
            , DigitiserCache digitiserCache
            , InstitutionCache institutionCache
            , PayloadTypeCache payloadTypeCache
            , PipelineCache pipelineCache
            , PreparationTypeCache preparationTypeCache
            , SubjectCache subjectCache
            , WorkstationCache workstationCache
            , ExtendableEnumService extendableEnumService
            , CollectionService collectionService
            , RoleService roleService) {
        this.collectionCache = collectionCache;
        this.digitiserCache = digitiserCache;
        this.institutionCache = institutionCache;
        this.payloadTypeCache = payloadTypeCache;
        this.pipelineCache = pipelineCache;
        this.preparationTypeCache = preparationTypeCache;
        this.subjectCache = subjectCache;
        this.workstationCache = workstationCache;
        this.extendableEnumService = extendableEnumService;
        this.collectionService = collectionService;
        this.roleService = roleService;
    }

    public Map<String, Object> getAllCaches() {
        Map<String, Object> allCaches = new HashMap<>();

        allCaches.put("institutions", institutionCache.getInstitutionMap());
        allCaches.put("collections", collectionCache.getCollectionMap());
        allCaches.put("digitisers", digitiserCache.getDigitiserMap());
        allCaches.put("payload_types", payloadTypeCache.getPayloadTypeMap());
        allCaches.put("pipelines", pipelineCache.getPipelineMap());
        allCaches.put("preparation_types", preparationTypeCache.getPreparationTypeMap());
        allCaches.put("restricted_access", Arrays.stream(InternalRole.values()).collect(Collectors.toMap(Enum::name, (x) -> x)));
        allCaches.put("status", extendableEnumService.getStatusCache());
        allCaches.put("subjects", subjectCache.getSubjectMap());
        allCaches.put("workstations", workstationCache.getWorkstationMap());
        logger.info("Institution Cache: {}", institutionCache.getInstitutions());
        logger.info("Collection Cache: {}", collectionCache.getCollectionMap());
        logger.info("Digitisers Cache: {}", digitiserCache.getDigitisers());
        logger.info("Payload Type Cache: {}", payloadTypeCache.getPayloadTypes());
        logger.info("Pipeline Cache: {}", pipelineCache.getPipelineMap());
        logger.info("Preparation Type Cache: {}", preparationTypeCache.getPreparationTypes());
        logger.info("Subject Cache: {}", subjectCache.getSubjects());
        logger.info("Workstation Cache: {}", workstationCache.getWorkstationMap());

        logger.info("Total number of caches added: {}", allCaches.size());

        return allCaches;
    }
    public void reloadCollections() {
        this.collectionService.initCollections(true);
    }

    public void reloadRoles() {
        this.roleService.initRoles(true);
    }
    public Map<String, Digitiser> getDigitiserMap() {
        return digitiserCache.getDigitiserMap();
    }


}
