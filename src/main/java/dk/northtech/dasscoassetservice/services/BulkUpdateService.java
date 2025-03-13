package dk.northtech.dasscoassetservice.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Strings;
import dk.northtech.dasscoassetservice.cache.DigitiserCache;
import dk.northtech.dasscoassetservice.cache.PayloadTypeCache;
import dk.northtech.dasscoassetservice.cache.PreparationTypeCache;
import dk.northtech.dasscoassetservice.cache.SubjectCache;
import dk.northtech.dasscoassetservice.configuration.FileProxyConfiguration;
import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.repositories.AssetRepository;
import dk.northtech.dasscoassetservice.repositories.AssetRepository2;
import dk.northtech.dasscoassetservice.repositories.BulkUpdateRepository;
import dk.northtech.dasscoassetservice.repositories.SpecimenRepository;
import dk.northtech.dasscoassetservice.webapi.domain.HttpAllocationStatus;
import dk.northtech.dasscoassetservice.webapi.domain.HttpInfo;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.inject.Inject;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class BulkUpdateService {
    private final InstitutionService institutionService;
    private final CollectionService collectionService;
    private final WorkstationService workstationService;
    private final StatisticsDataServiceV2 statisticsDataServiceV2;
    private final FileProxyClient fileProxyClient;
    private final PipelineService pipelineService;
    private final RightsValidationService rightsValidationService;
    private final Jdbi jdbi;
    private final DigitiserCache digitiserCache;
    private final SubjectCache subjectCache;
    private final PayloadTypeCache payloadTypeCache;
    private final PreparationTypeCache preparationTypeCache;
    private static final Logger logger = LoggerFactory.getLogger(BulkUpdateService.class);
    private final FileProxyConfiguration fileProxyConfiguration;
    private final ObservationRegistry observationRegistry;
    private final ExtendableEnumService extendableEnumService;
    Cache<String, Instant> assetsGettingCreated;

    @Inject
    public BulkUpdateService(InstitutionService institutionService
            , CollectionService collectionService
            , WorkstationService workstationService
            , @Lazy FileProxyClient fileProxyClient
            , Jdbi jdbi
            , StatisticsDataServiceV2 statisticsDataServiceV2
            , PipelineService pipelineService
            , RightsValidationService rightsValidationService
            , DigitiserCache digitiserCache
            , SubjectCache subjectCache
            , PayloadTypeCache payloadTypeCache
            , PreparationTypeCache preparationTypeCache
            , FileProxyConfiguration fileProxyConfiguration
            , ObservationRegistry observationRegistry
            , ExtendableEnumService extendableEnumService) {
        this.institutionService = institutionService;
        this.collectionService = collectionService;
        this.workstationService = workstationService;
        this.fileProxyClient = fileProxyClient;
        this.statisticsDataServiceV2 = statisticsDataServiceV2;
        this.pipelineService = pipelineService;
        this.jdbi = jdbi;
        this.digitiserCache = digitiserCache;
        this.subjectCache = subjectCache;
        this.payloadTypeCache = payloadTypeCache;
        this.rightsValidationService = rightsValidationService;
        this.preparationTypeCache = preparationTypeCache;
        this.fileProxyConfiguration = fileProxyConfiguration;
        this.observationRegistry = observationRegistry;
        this.extendableEnumService = extendableEnumService;
        this.assetsGettingCreated = Caffeine.newBuilder()
                .expireAfterWrite(fileProxyConfiguration.shareCreationBlockedSeconds(), TimeUnit.SECONDS).build();
    }

    public List<Asset> bulkUpdate(List<String> assetList, Asset updatedAsset, User user) {
               /* Bulk-Updatable fields:
            Tags (Added, not replaced).
            Status
            Asset_Locked (Only for locking, not unlocking)
            Subject ️
            Funding
            Payload_Type
            Parent_Guid
            Digitiser
        */

//        // Validation
        if (updatedAsset == null) {
            throw new IllegalArgumentException("Empty body, please specify fields to update");
        }
        if (Strings.isNullOrEmpty(updatedAsset.updateUser)) {
            throw new IllegalArgumentException("Update user must be provided!");
        }
        if (assetList.isEmpty()) {
            throw new IllegalArgumentException("Assets to update cannot be empty.");
        }

        // Check if all the assets exist:
        List<Asset> assets = jdbi.onDemand(BulkUpdateRepository.class).readMultipleAssets(assetList);
        for(Asset a : assets) {
            System.out.println(a);
        }

        if (assets.size() != assetList.size()) {
            throw new IllegalArgumentException("One or more assets were not found!");
        }

        for (Asset asset : assets) {
            rightsValidationService.checkWriteRightsThrowing(user, asset.institution, asset.collection);
        }


        // Check that the bulk update will not set the asset to be its own parent:
        for (Asset asset : assets) {
            if (updatedAsset.parent_guid != null) {
                if (asset.asset_guid.equals(updatedAsset.parent_guid)) {
                    throw new IllegalArgumentException("Asset cannot be its own parent");
                }
            }
        }

        // Parent_guid does not exist:
        if (updatedAsset.parent_guid != null) {
            Optional<Asset> optParent = this.getAsset(updatedAsset.parent_guid);
            if (optParent.isEmpty()) {
                throw new IllegalArgumentException("asset_parent does not exist!");
            }
        }

        // Do not allow unlocking:
        if (!updatedAsset.asset_locked) {
            for (Asset asset : assets) {
                if (asset.asset_locked) {
                    throw new DasscoIllegalActionException("Cannot unlock using updateAsset API, use dedicated API for unlocking");
                }
            }
        }

        String sql = this.bulkUpdateSqlStatementFactory(assetList, updatedAsset);
        AgtypeMapBuilder builder = this.bulkUpdateBuilderFactory(updatedAsset);

//        // Create the new BULK_UPDATE_ASSET_METADATA event:
//        Event event = new Event();
//        event.event = DasscoEvent.BULK_UPDATE_ASSET_METADATA;
//        event.user = updatedAsset.updateUser;
//        event.workstation = updatedAsset.workstation;
//        event.pipeline = updatedAsset.pipeline;
//        event.timeStamp = Instant.now();
//
//        assets.forEach(asset -> {
//            Map<String, String> existingTags = new HashMap<>(asset.tags);
//            for (Map.Entry<String, String> entry : updatedAsset.tags.entrySet()) {
//                if (existingTags.containsKey(entry.getKey())) {
//                    if (!existingTags.get(entry.getKey()).equals(entry.getValue())) {
//                        existingTags.put(entry.getKey(), entry.getValue());
//                    }
//                } else {
//                    existingTags.put(entry.getKey(), entry.getValue());
//                }
//            }
//            asset.tags = existingTags;
//        });
//
//        List<Asset> bulkUpdateSuccess = jdbi.onDemand(AssetRepository2.class).bulkUpdate(sql, builder, updatedAsset, event, assets, assetList);
//
//        logger.info("Adding Digitiser to Cache if absent in Bulk Update Asset Method");
//        digitiserCache.putDigitiserInCacheIfAbsent(new Digitiser(updatedAsset.updateUser, updatedAsset.updateUser));
//
//        if (updatedAsset.subject != null && !updatedAsset.subject.isEmpty()) {
//            if (!subjectCache.getSubjectMap().containsKey(updatedAsset.subject)) {
//                this.subjectCache.clearCache();
//                List<String> subjectList = jdbi.withHandle(handle -> {
//                    AssetRepository assetRepository = handle.attach(AssetRepository.class);
//                    return assetRepository.listSubjects();
//                });
//                if (!subjectList.isEmpty()) {
//                    for (String subject : subjectList) {
//                        this.subjectCache.putSubjectsInCacheIfAbsent(subject);
//                    }
//                }
//            }
//        }
//
//        if (updatedAsset.payload_type != null && !updatedAsset.payload_type.isEmpty()) {
//            if (!payloadTypeCache.getPayloadTypeMap().containsKey(updatedAsset.payload_type)) {
//                this.payloadTypeCache.clearCache();
//                List<String> payloadTypeList = jdbi.withHandle(handle -> {
//                    AssetRepository assetRepository = handle.attach(AssetRepository.class);
//                    return assetRepository.listPayloadTypes();
//                });
//                if (!payloadTypeList.isEmpty()) {
//                    for (String payloadType : payloadTypeList) {
//                        this.payloadTypeCache.putPayloadTypesInCacheIfAbsent(payloadType);
//                    }
//                }
//            }
//        }
//        return bulkUpdateSuccess;
    return null;
    }

    AgtypeMapBuilder bulkUpdateBuilderFactory(Asset updatedFields) {
        AgtypeMapBuilder builder = new AgtypeMapBuilder();

        if (updatedFields.status != null) {
            builder.add("status", updatedFields.status);
        }
        //TODO handle new lists here
//        if (updatedFields.funding != null) {
//            builder.add("funding", updatedFields.funding);
//        }

        if (updatedFields.subject != null) {
            builder.add("subject", updatedFields.subject);
        }

        if (updatedFields.payload_type != null) {
            builder.add("payload_type", updatedFields.payload_type);
        }

        if (updatedFields.parent_guid != null) {
            builder.add("parent_id", updatedFields.parent_guid);
        }

        if (updatedFields.asset_locked) {
            builder.add("asset_locked", true);
        }

        if (updatedFields.digitiser != null) {
            builder.add("digitiser", updatedFields.digitiser);
        }

        builder
                .add("user", updatedFields.updateUser);

        return builder;
    }

    String bulkUpdateSqlStatementFactory(List<String> assetList, Asset updatedFields) {

        String assetListAsString = assetList.stream()
                .map(asset -> "'" + asset + "'")
                .collect(Collectors.joining(", "));

        String sql = """
                SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (a:Asset)
                            WHERE a.asset_guid IN [%s]
                            
                            SET
                """;

        if (updatedFields.funding != null) {
            sql = sql + """
                                a.funding = $funding,
                    """;
        }
        if (updatedFields.subject != null) {
            sql = sql + """
                                a.subject = $subject,
                    """;
        }
        if (updatedFields.payload_type != null) {
            sql = sql + """
                                a.payload_type = $payload_type,
                    """;
        }

        if (updatedFields.status != null) {
            sql = sql + """
                                a.status = $status,
                    """;
        }
        if (updatedFields.parent_guid != null) {
            sql = sql + """
                                a.parent_id = $parent_id,
                    """;
        }
        // If asset locked is false it means either that they forgot to add it or that they want to unlock an asset, which they cannot do like this.
        if (updatedFields.asset_locked) {
            sql = sql + """
                                a.asset_locked = $asset_locked,
                    """;
        }
        if (updatedFields.digitiser != null) {
            sql = sql + """
                                a.digitiser = $digitiser,
                    """;
        }

        sql = sql + """
                        a.workstation = $workstation_name,
                        a.pipeline = $pipeline_name
                        $$
                        , #params) as (a agtype);
                """;

        return sql.formatted(assetListAsString);
    }

    void validateAssetFields(Asset asset) {
        if (Strings.isNullOrEmpty(asset.asset_guid)) {
            throw new IllegalArgumentException("asset_guid cannot be null");
        }
        if (Strings.isNullOrEmpty(asset.asset_pid)) {
            throw new IllegalArgumentException("asset_pid cannot be null");
        }
        if (Strings.isNullOrEmpty(asset.status) ||!extendableEnumService.getStatuses().contains(asset.status)) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        if ("".equals(asset.parent_guid)) {
            throw new IllegalArgumentException("Parent may not be an empty string");
        }
    }

    void validateAsset(Asset asset) {
        Optional<Pipeline> pipelineOpt = pipelineService.findPipelineByInstitutionAndName(asset.pipeline, asset.institution);
        if (pipelineOpt.isEmpty()) {
            throw new IllegalArgumentException("Pipeline doesnt exist in this institution");
        }
        if (asset.asset_guid.equals(asset.parent_guid)) {
            throw new IllegalArgumentException("Asset cannot be its own parent");
        }
        Optional<Workstation> workstationOpt = workstationService.findWorkstation(asset.workstation);
        if (workstationOpt.isEmpty()) {
            throw new IllegalArgumentException("Workstation does not exist");
        }
        Workstation workstation = workstationOpt.get();
        if (workstation.status().equals(WorkstationStatus.OUT_OF_SERVICE)) {
            throw new DasscoIllegalActionException("Workstation [" + workstation.status() + "] is marked as out of service");
        }
        if(asset.file_formats != null && !asset.file_formats.isEmpty()){
            Set<String> fileFormats = extendableEnumService.getFileFormats();
            for(String s: fileFormats) {
                if(!fileFormats.contains(s)){
                    throw new IllegalArgumentException(s + " is not a valid file format");
                }
            }
        }
        if (asset.parent_guid != null) {
            Optional<Asset> parentOpt = getAsset(asset.parent_guid);
            if (parentOpt.isEmpty()) {
                throw new IllegalArgumentException("Parent doesnt exist");
            }
        }

    }


    public void refreshCaches(Asset asset) {
        LocalDateTime cacheStart = LocalDateTime.now();
        if (asset.digitiser != null && !asset.digitiser.isEmpty()) {
            logger.info("Adding Digitiser to Cache if absent in Persist Asset Method");
            digitiserCache.putDigitiserInCacheIfAbsent(new Digitiser(asset.digitiser, asset.digitiser));
        }

        if (asset.specimens != null && !asset.specimens.isEmpty()) {
            for (Specimen specimen : asset.specimens) {
                preparationTypeCache.putPreparationTypesInCacheIfAbsent(specimen.preparation_type());
            }
        }

        if (asset.subject != null && !asset.subject.isEmpty()) {
            subjectCache.putSubjectsInCacheIfAbsent(asset.subject);
        }

        if (asset.payload_type != null && !asset.payload_type.isEmpty()) {
            payloadTypeCache.putPayloadTypesInCacheIfAbsent(asset.payload_type);
        }
        LocalDateTime cacheEnd = LocalDateTime.now();
        logger.info("#7 Refreshing dropdown caches took {} ms", Duration.between(cacheStart, cacheEnd).toMillis());
    }

    public List<String> listSubjects() {
        return subjectCache.getSubjects();
    }

    public List<Digitiser> listDigitisers() {
        return digitiserCache.getDigitisers();
    }

    public List<String> listPayloadTypes() {
        return payloadTypeCache.getPayloadTypes();
    }

    public void reloadAssetCache() {
        subjectCache.clearCache();
        List<String> subjectList = jdbi.withHandle(handle -> {
            AssetRepository assetRepository = handle.attach(AssetRepository.class);
            return assetRepository.listSubjects();
        });
        if (!subjectList.isEmpty()) {
            for (String subject : subjectList) {
                this.subjectCache.putSubjectsInCacheIfAbsent(subject);
            }
        }
        payloadTypeCache.clearCache();
        List<String> payloadTypeList = jdbi.withHandle(handle -> {
            AssetRepository assetRepository = handle.attach(AssetRepository.class);
            return assetRepository.listPayloadTypes();
        });
        if (!payloadTypeList.isEmpty()) {
            for (String payloadType : payloadTypeList) {
                this.payloadTypeCache.putPayloadTypesInCacheIfAbsent(payloadType);
            }
        }
        preparationTypeCache.clearCache();
        List<String> preparationTypeList = jdbi.withHandle(handle -> {
            SpecimenRepository specimenRepository = handle.attach(SpecimenRepository.class);
            return specimenRepository.listPreparationTypes();
        });
        if (!preparationTypeList.isEmpty()) {
            for (String preparationType : preparationTypeList) {
                this.preparationTypeCache.putPreparationTypesInCacheIfAbsent(preparationType);
            }
        }
    }

    //This is here for mocking
    public HttpInfo openHttpShare(MinimalAsset minimalAsset, User updateUser, int allocation) {
        return fileProxyClient.openHttpShare(minimalAsset, updateUser, allocation);
    }

    public Optional<Asset> getAsset(String assetGuid) {
        return jdbi.onDemand(AssetRepository.class).readAsset(assetGuid);
    }

    public Optional<Asset> checkUserRights(String assetGuid, User user) {
        LocalDateTime getAssetStart = LocalDateTime.now();
        Optional<Asset> optionalAsset = jdbi.onDemand(AssetRepository.class).readAsset(assetGuid);
        LocalDateTime getAssetEnd = LocalDateTime.now();
        logger.info("#4.1.2 Getting complete asset from the DB took {} ms", Duration.between(getAssetStart, getAssetEnd).toMillis());
        if (optionalAsset.isPresent()) {
            Asset found = optionalAsset.get();
            LocalDateTime checkValidationStart = LocalDateTime.now();
            rightsValidationService.checkReadRightsThrowing(user, found.institution, found.collection, found.asset_guid);
            LocalDateTime checkValidationEnd = LocalDateTime.now();
            logger.info("#4.1.3 Validating Asset took {} ms", Duration.between(checkValidationStart, checkValidationEnd).toMillis());
        }
        return optionalAsset;
    }

    public List<Asset> readMultipleAssets(List<String> assets) {
        return jdbi.onDemand(AssetRepository.class).readMultipleAssets(assets);
    }

    // For Mocking.
    public HttpClient createHttpClient() {
        return HttpClient.newHttpClient();
    }

}
