package dk.northtech.dasscoassetservice.services;

import com.google.inject.Inject;
import dk.northtech.dasscoassetservice.cache.*;
import dk.northtech.dasscoassetservice.domain.Digitiser;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CacheService {
    private CollectionCache collectionCache;
    private DigitiserCache digitiserCache;
    private InstitutionCache institutionCache;
    private PayloadTypeCache payloadTypeCache;
    private PipelineCache pipelineCache;
    private PreparationTypeCache preparationTypeCache;
    private RestrictedAccessCache restrictedAccessCache;
    private StatusCache statusCache;
    private SubjectCache subjectCache;
    private WorkstationCache workstationCache;

    @Inject
    public CacheService(CollectionCache collectionCache, DigitiserCache digitiserCache,
                        InstitutionCache institutionCache, PayloadTypeCache payloadTypeCache,
                        PipelineCache pipelineCache, PreparationTypeCache preparationTypeCache,
                        RestrictedAccessCache restrictedAccessCache,
                        StatusCache statusCache, SubjectCache subjectCache,
                        WorkstationCache workstationCache){
        this.collectionCache = collectionCache;
        this.digitiserCache = digitiserCache;
        this.institutionCache = institutionCache;
        this.payloadTypeCache = payloadTypeCache;
        this.pipelineCache = pipelineCache;
        this.preparationTypeCache = preparationTypeCache;
        this.restrictedAccessCache = restrictedAccessCache;
        this.statusCache = statusCache;
        this.subjectCache = subjectCache;
        this.workstationCache = workstationCache;

    }

    public Map<String, Object> getAllCaches(){
        Map<String, Object> allCaches = new HashMap<>();

        allCaches.put("institutions", institutionCache.getInstitutionMap());
        allCaches.put("collections", collectionCache.getCollectionMap());
        allCaches.put("digitisers", digitiserCache.getDigitiserMap());
        allCaches.put("payload_types", payloadTypeCache.getPayloadTypeMap());
        allCaches.put("pipelines", pipelineCache.getPipelineCache());
        allCaches.put("preparation_types", preparationTypeCache.getPreparationType());
        allCaches.put("restricted_access", restrictedAccessCache.getRestrictedAccessMap());
        allCaches.put("status", statusCache.getStatusMap());
        allCaches.put("subjects", subjectCache.getSubjectMap());
        allCaches.put("workstations", workstationCache.getWorkstationMap());

        return allCaches;
    }

    public Map<String, Digitiser> getDigitiserMap(){
        return digitiserCache.getDigitiserMap();
    }
}
