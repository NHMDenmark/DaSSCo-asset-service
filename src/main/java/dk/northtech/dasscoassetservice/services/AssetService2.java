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
public class AssetService2 {
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
    private static final Logger logger = LoggerFactory.getLogger(AssetService2.class);
    private final FileProxyConfiguration fileProxyConfiguration;
    private final ObservationRegistry observationRegistry;
    private final ExtendableEnumService extendableEnumService;
    Cache<String, Instant> assetsGettingCreated;

    @Inject
    public AssetService2(InstitutionService institutionService
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

    void validateNewAsset(Asset asset) {
        if (Strings.isNullOrEmpty(asset.institution)) {
            throw new IllegalArgumentException("Institution cannot be null");
        }
        if (Strings.isNullOrEmpty(asset.collection)) {
            throw new IllegalArgumentException("Collection cannot be null");
        }
        Optional<Institution> ifExists = institutionService.getIfExists(asset.institution);
        if (ifExists.isEmpty()) {
            throw new IllegalArgumentException("Institution doesnt exist");
        }
        if (Strings.isNullOrEmpty(asset.digitiser)) {
            throw new IllegalArgumentException("digitiser cannot be null when creating asset");
        }
        Optional<Collection> collectionOpt = collectionService.findCollectionInternal(asset.collection, asset.institution);
        if (collectionOpt.isEmpty()) {
            throw new IllegalArgumentException("Collection doesnt exist");
        }
    }

    public Optional<Asset> getAsset(String assetGuid) {
        return jdbi.onDemand(AssetRepository2.class).readAsset(assetGuid);
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

    public Asset persistAsset(Asset asset, User user, int allocation) {
        //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        validateAssetFields(asset);
        if (assetsGettingCreated.getIfPresent(asset.asset_guid) != null) {
            logger.warn("Same asset uploaded in short time frame, guid: {}", asset.asset_guid);
            throw new DasscoIllegalActionException("Asset " + asset.asset_guid + " is already being processed");
        }
        //Validation
        LocalDateTime databaseCheckStart = LocalDateTime.now();
        Observation.createNotStarted("persist:checkAssetExists", observationRegistry).observe(() -> {
            Optional<Asset> assetOpt = getAsset(asset.asset_guid);
            if (assetOpt.isPresent()) {
                throw new IllegalArgumentException("Asset " + asset.asset_guid + " already exists");
            }
        });
        if (allocation == 0) {
            throw new IllegalArgumentException("Allocation cannot be 0");
        }
        LocalDateTime databaseCheckEnd = LocalDateTime.now();
        logger.info("#2: Database call to check if Asset existed took {} ms", Duration.between(databaseCheckStart, databaseCheckEnd).toMillis());
        LocalDateTime validationStart = LocalDateTime.now();
        rightsValidationService.checkWriteRights(user, asset.institution, asset.collection);
        validateNewAsset(asset);
        validateAsset(asset);
        // Create share
        assetsGettingCreated.put(asset.asset_guid, Instant.now());
        LocalDateTime validationEnd = LocalDateTime.now();
        logger.info("#3: Validation took {} ms (Check Write Rights, Validate Asset Fields, Validate Asset)", Duration.between(validationStart, validationEnd).toMillis());

        LocalDateTime httpInfoStart = LocalDateTime.now();
        logger.info("POSTing asset {} with parent {} to file-proxy", asset.asset_guid, asset.parent_guid);
        Asset resultAsset = jdbi.inTransaction(h -> {
            // Default values on creation
            asset.date_metadata_updated = Instant.now();
            asset.created_date = Instant.now();
            asset.internal_status = InternalStatus.METADATA_RECEIVED;
            LocalDateTime createAssetStart = LocalDateTime.now();
            // Create the asset
            Observation.createNotStarted("persist:create-asset", observationRegistry).observe(() -> {
                jdbi.onDemand(AssetRepository2.class)
                        .createAsset(asset);
            });
            LocalDateTime createAssetEnd = LocalDateTime.now();
            logger.info("#5 Creating the asset took {} ms", Duration.between(createAssetStart, createAssetEnd).toMillis());
            // Open share
            try {
                Observation.createNotStarted("persist:openShareOnFP", observationRegistry)
                        .observe(() -> {
                            asset.httpInfo = openHttpShare(new MinimalAsset(asset.asset_guid, asset.parent_guid, asset.institution, asset.collection), user, allocation);
                        });
            } catch (Exception e) {
                h.rollback();
                throw new RuntimeException(e);
            }
            LocalDateTime httpInfoEnd = LocalDateTime.now();
            logger.info("#4 HTTPInfo creation took {} ms in total.", Duration.between(httpInfoStart, httpInfoEnd).toMillis());

            if (asset.httpInfo.http_allocation_status() == HttpAllocationStatus.SUCCESS) {

                LocalDateTime refreshCachedDataStart = LocalDateTime.now();
                //TEZT
                Observation.createNotStarted("persist:refresh-statistics-cache", observationRegistry)
                        .observe(statisticsDataServiceV2::refreshCachedData);
                LocalDateTime refreshCachedDataEnd = LocalDateTime.now();
                logger.info("#6 Refreshing the cached data took {} ms", Duration.between(refreshCachedDataStart, refreshCachedDataEnd).toMillis());

//            this.statisticsDataService.addAssetToCache(asset);
                refreshCaches(asset);
            } else {
                // Do not persist asset if share wasnt created
                h.rollback();
                assetsGettingCreated.invalidate(asset.asset_guid);
                return asset;
            }
            //you are here
            return asset;
        });


        statisticsDataServiceV2.refreshCachedData();
//        this.statisticsDataServiceV2.addAssetToCache(asset);
        assetsGettingCreated.invalidate(asset.asset_guid);
        return resultAsset;
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



    public Asset updateAsset(Asset updatedAsset, User user) {
        Optional<Asset> assetOpt = getAsset(updatedAsset.asset_guid);
        if (assetOpt.isEmpty()) {
            throw new IllegalArgumentException("Asset " + updatedAsset.asset_guid + " does not exist");
        }
        if (Strings.isNullOrEmpty(updatedAsset.updateUser)) {
            throw new IllegalArgumentException("Update user must be provided");
        }
        validateAsset(updatedAsset);
        Asset existing = assetOpt.get();
        rightsValidationService.checkWriteRightsThrowing(user, existing.institution, existing.collection);
        Set<String> updatedSpecimenBarcodes = updatedAsset.specimens.stream().map(Specimen::barcode).collect(Collectors.toSet());

        List<Specimen> specimensToDetach = existing.specimens.stream().filter(s -> !updatedSpecimenBarcodes.contains(s.barcode())).collect(Collectors.toList());
        existing.specimens = updatedAsset.specimens;
        existing.tags = updatedAsset.tags;
        existing.workstation = updatedAsset.workstation;
        existing.pipeline = updatedAsset.pipeline;
        existing.date_asset_finalised = updatedAsset.date_asset_finalised;
        existing.status = updatedAsset.status;
        if (existing.asset_locked && !updatedAsset.asset_locked) {
            throw new DasscoIllegalActionException("Cannot unlock using updateAsset API, use dedicated API for unlocking");
        }
        existing.asset_locked = updatedAsset.asset_locked;
        existing.subject = updatedAsset.subject;
        existing.restricted_access = updatedAsset.restricted_access;
        existing.file_formats = updatedAsset.file_formats;
        existing.payload_type = updatedAsset.payload_type;
        existing.parent_guid = updatedAsset.parent_guid;
        existing.updateUser = updatedAsset.updateUser;
        existing.asset_pid = updatedAsset.asset_pid == null ? existing.asset_pid : updatedAsset.asset_pid;
        existing.metadata_version = updatedAsset.metadata_version;
        existing.metadata_source = updatedAsset.metadata_source;
        existing.camera_setting_control = updatedAsset.camera_setting_control;
        existing.push_to_specify = updatedAsset.push_to_specify;
        existing.digitiser = updatedAsset.digitiser;
        existing.make_public = updatedAsset.make_public;

        existing.issues = updatedAsset.issues;
        existing.complete_digitiser_list = updatedAsset.complete_digitiser_list;
        existing.funding = updatedAsset.funding;

        validateAssetFields(existing);
        jdbi.onDemand(AssetRepository2.class)
                .updateAsset(existing, specimensToDetach);

        statisticsDataServiceV2.refreshCachedData();

        logger.info("Adding Digitiser to Cache if absent in Update Asset Method");
        digitiserCache.putDigitiserInCacheIfAbsent(new Digitiser(updatedAsset.updateUser, updatedAsset.updateUser));


        if (updatedAsset.subject != null && !updatedAsset.subject.isEmpty()) {
            if (!subjectCache.getSubjectMap().containsKey(updatedAsset.subject)) {
                this.subjectCache.clearCache();
                List<String> subjectList = jdbi.withHandle(handle -> {
                    AssetRepository assetRepository = handle.attach(AssetRepository.class);
                    return assetRepository.listSubjects();
                });
                if (!subjectList.isEmpty()) {
                    for (String subject : subjectList) {
                        this.subjectCache.putSubjectsInCacheIfAbsent(subject);
                    }
                }
            }
        }

        if (updatedAsset.payload_type != null && !updatedAsset.payload_type.isEmpty()) {
            if (!payloadTypeCache.getPayloadTypeMap().containsKey(updatedAsset.payload_type)) {
                this.payloadTypeCache.clearCache();
                List<String> payloadTypeList = jdbi.withHandle(handle -> {
                    AssetRepository assetRepository = handle.attach(AssetRepository.class);
                    return assetRepository.listPayloadTypes();
                });
                if (!payloadTypeList.isEmpty()) {
                    for (String payloadType : payloadTypeList) {
                        this.payloadTypeCache.putPayloadTypesInCacheIfAbsent(payloadType);
                    }
                }
            }
        }

        boolean prepTypeExists = true;
        if (updatedAsset.specimens != null && !updatedAsset.specimens.isEmpty()) {
            for (Specimen specimen : updatedAsset.specimens) {
                if (!preparationTypeCache.getPreparationTypeMap().containsKey(specimen.preparation_type())) {
                    prepTypeExists = false;
                    break;
                }
            }
            if (!prepTypeExists) {
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
        }
        return existing;
    }
}
