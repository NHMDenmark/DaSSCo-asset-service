package dk.northtech.dasscoassetservice.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Strings;
import dk.northtech.dasscoassetservice.cache.DigitiserCache;
import dk.northtech.dasscoassetservice.cache.PayloadTypeCache;
import dk.northtech.dasscoassetservice.cache.PreparationTypeCache;
import dk.northtech.dasscoassetservice.cache.SubjectCache;
import dk.northtech.dasscoassetservice.configuration.FileProxyConfiguration;
import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.repositories.AssetRepository;
import dk.northtech.dasscoassetservice.repositories.BulkUpdateRepository;
import io.micrometer.observation.ObservationRegistry;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class BulkUpdateService {
    private final WorkstationService workstationService;
    private final PipelineService pipelineService;
    private final RightsValidationService rightsValidationService;
    private final Jdbi jdbi;
    private final DigitiserCache digitiserCache;
    private final SubjectCache subjectCache;
    private final PayloadTypeCache payloadTypeCache;

    private static final Logger logger = LoggerFactory.getLogger(BulkUpdateService.class);

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
        this.workstationService = workstationService;

        this.pipelineService = pipelineService;
        this.jdbi = jdbi;
        this.digitiserCache = digitiserCache;
        this.subjectCache = subjectCache;
        this.payloadTypeCache = payloadTypeCache;
        this.rightsValidationService = rightsValidationService;
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
        if (assetList.isEmpty()) {
            throw new IllegalArgumentException("Assets to update cannot be empty.");
        }

        // Check if all the assets exist:
        List<Asset> assets = jdbi.onDemand(BulkUpdateRepository.class).readMultipleAssets(assetList);
        for (Asset a : assets) {
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

            if (asset.parent_guids.contains(asset.asset_guid)) {
                throw new IllegalArgumentException("Asset cannot be its own parent");
            }
        }

        // Parent_guid does not exist:

        updatedAsset.parent_guids.forEach(p -> {

            Optional<Asset> optParent = this.getAsset(p);
            if (optParent.isEmpty()) {
                throw new IllegalArgumentException("asset_parent does not exist!");
            }
        });


        // Do not allow unlocking:
        if (!updatedAsset.asset_locked) {
            for (Asset asset : assets) {
                if (asset.asset_locked) {
                    throw new DasscoIllegalActionException("Cannot unlock using updateAsset API, use dedicated API for unlocking");
                }
            }
        }

//        String sql = this.bulkUpdateSqlStatementFactory(assetList, updatedAsset);
//        AgtypeMapBuilder builder = this.bulkUpdateBuilderFactory(updatedAsset);

        // Create the new BULK_UPDATE_ASSET_METADATA event:
        Event event = new Event();
        event.event = DasscoEvent.BULK_UPDATE_ASSET_METADATA;
        event.user = user.username;
        event.pipeline = updatedAsset.pipeline;
        event.timestamp = Instant.now();
//
        assets.forEach(asset -> {
            Map<String, String> existingTags = new HashMap<>(asset.tags);
            for (Map.Entry<String, String> entry : updatedAsset.tags.entrySet()) {
                if (existingTags.containsKey(entry.getKey())) {
                    if (!existingTags.get(entry.getKey()).equals(entry.getValue())) {
                        existingTags.put(entry.getKey(), entry.getValue());
                    }
                } else {
                    existingTags.put(entry.getKey(), entry.getValue());
                }
            }
            asset.tags = existingTags;
        });

        List<Asset> bulkUpdateSuccess = jdbi.onDemand(BulkUpdateRepository.class).bulkUpdate(updatedAsset, event, assets, assetList);

        logger.info("Adding Digitiser to Cache if absent in Bulk Update Asset Method");
        digitiserCache.putDigitiserInCacheIfAbsent(new Digitiser(updatedAsset.digitiser, updatedAsset.digitiser));

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
        return bulkUpdateSuccess;

    }




    public Optional<Asset> getAsset(String assetGuid) {
        return jdbi.onDemand(AssetRepository.class).readAsset(assetGuid);
    }



    public List<Asset> readMultipleAssets(List<String> assets) {
        return jdbi.onDemand(BulkUpdateRepository.class).readMultipleAssets(assets);
    }

    public String createCSVString(List<Asset> assets) {
        String csv = "";
        if (assets.isEmpty()) {
            return "";
        }
        StringBuilder csvBuilder = new StringBuilder();
        Field[] fields = Asset.class.getDeclaredFields();
        String headers = String.join(",", Arrays.stream(fields).map(Field::getName).collect(Collectors.toList()));
        csvBuilder.append(headers).append("\n");
        for (Asset asset : assets) {
            StringJoiner joiner = new StringJoiner(",");
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    Object value = field.get(asset);
                    joiner.add(escapeCsvValue(value != null ? value.toString() : ""));
                } catch (IllegalAccessException e) {
                    joiner.add("");
                }
            }
            csvBuilder.append(joiner).append("\n");
        }
        return csvBuilder.toString();
    }

    public String escapeCsvValue(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // For Mocking.
    public HttpClient createHttpClient() {
        return HttpClient.newHttpClient();
    }

}
