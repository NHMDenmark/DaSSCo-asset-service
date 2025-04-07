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
import dk.northtech.dasscoassetservice.repositories.*;
import dk.northtech.dasscoassetservice.webapi.domain.HttpAllocationStatus;
import dk.northtech.dasscoassetservice.webapi.domain.HttpInfo;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
public class AssetService {
    private final InstitutionService institutionService;
    private final CollectionService collectionService;
    private final WorkstationService workstationService;
    private final StatisticsDataServiceV2 statisticsDataServiceV2;
    private final FileProxyClient fileProxyClient;
    private final PipelineService pipelineService;
    private final RightsValidationService rightsValidationService;
    private final Jdbi jdbi;
    private final FundingService fundingService;
    private final DigitiserCache digitiserCache;
    private final SubjectCache subjectCache;
    private final PayloadTypeCache payloadTypeCache;
    private final PreparationTypeCache preparationTypeCache;
    private static final Logger logger = LoggerFactory.getLogger(AssetService.class);
    private final FileProxyConfiguration fileProxyConfiguration;
    private final ObservationRegistry observationRegistry;
    private final ExtendableEnumService extendableEnumService;
    Cache<String, Instant> assetsGettingCreated;
    private final UserService userService;

    @Inject
    public AssetService(InstitutionService institutionService
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
            , ExtendableEnumService extendableEnumService
            , UserService userService
            , FundingService fundingService) {
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
        this.userService = userService;
        this.fundingService = fundingService;
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
        if (Strings.isNullOrEmpty(asset.status) || !extendableEnumService.getStatuses().contains(asset.status)) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        if ("".equals(asset.parent_guid)) {
            throw new IllegalArgumentException("Parent may not be an empty string");
        }
    }

    void valiedateAndSetCollectionId(Asset asset) {
        Optional<Collection> collectionOpt = collectionService.findCollectionInternal(asset.collection, asset.institution);
        if (collectionOpt.isEmpty()) {
            throw new IllegalArgumentException("Collection doesnt exist");
        }
        asset.collection_id = collectionOpt.get().collection_id();
    }

    void validateNewAssetAndSetIds(Asset asset) {
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

    }

    public Optional<Asset> getAsset(String assetGuid) {
        Optional<Asset> asset = jdbi.onDemand(AssetRepository.class).readAssetInternalNew(assetGuid);
        if (asset.isPresent()) {
            Asset assetToBeMapped = asset.get();
            jdbi.withHandle(h -> {
                EventRepository attach = h.attach(EventRepository.class);
                assetToBeMapped.events = attach.getAssetEvents(assetGuid);
                SpecimenRepository specimenRepository = h.attach(SpecimenRepository.class);
                assetToBeMapped.specimens = specimenRepository.findSpecimensByAsset(assetGuid);
                UserRepository userRepository = h.attach(UserRepository.class);
                assetToBeMapped.complete_digitiser_list = userRepository.getDigitiserList(assetGuid);
                FundingRepository fundingRepository = h.attach(FundingRepository.class);
                assetToBeMapped.funding = fundingRepository.getAssetFunds(assetToBeMapped.asset_guid).stream().map(Funding::funding).toList();
                return h;
            });
            for (Event event : assetToBeMapped.events) {
                System.out.println(event);
                if (DasscoEvent.AUDIT_ASSET.equals(event.event)) {
                    assetToBeMapped.audited = true;
                } else if (DasscoEvent.BULK_UPDATE_ASSET_METADATA.equals(event.event) && assetToBeMapped.date_metadata_updated == null) {
                    assetToBeMapped.date_metadata_updated = event.timestamp;
                } else if (DasscoEvent.UPDATE_ASSET_METADATA.equals(event.event) && assetToBeMapped.date_metadata_updated == null) {
                    assetToBeMapped.date_metadata_updated = event.timestamp;
                } else if (DasscoEvent.CREATE_ASSET_METADATA.equals(event.event)) {
                    if (assetToBeMapped.date_metadata_updated == null) {
                        assetToBeMapped.date_metadata_updated = event.timestamp;
                    }
                    //The pipeline field is always taken from the create event, even if later updates are present with different pipeline
                    assetToBeMapped.pipeline = event.pipeline;
                } else if (DasscoEvent.DELETE_ASSET_METADATA.equals(event.event)) {
                    assetToBeMapped.date_asset_deleted = event.timestamp;
                }
            }
            return Optional.of(assetToBeMapped);
        }
        return asset;
    }

    void validateAsset(Asset asset) {

        if (asset.asset_guid.equals(asset.parent_guid)) {
            throw new IllegalArgumentException("Asset cannot be its own parent");
        }
        Optional<Pipeline> pipelineOpt = pipelineService.findPipelineByInstitutionAndName(asset.pipeline, asset.institution);
        if (pipelineOpt.isEmpty()) {
            throw new IllegalArgumentException("Pipeline doesnt exist in this institution");
        }
        Optional<Workstation> workstationOpt = workstationService.findWorkstation(asset.workstation, asset.institution);
        if (workstationOpt.isEmpty()) {
            throw new IllegalArgumentException("Workstation does not exist");
        }
        Workstation workstation = workstationOpt.get();
        if (workstation.status().equals(WorkstationStatus.OUT_OF_SERVICE)) {
            throw new DasscoIllegalActionException("Workstation [" + workstation.status() + "] is marked as out of service");
        }
        asset.workstation_id = workstation.workstation_id();
        if (asset.file_formats != null && !asset.file_formats.isEmpty()) {
            Set<String> fileFormats = extendableEnumService.getFileFormats();
            for (String s : fileFormats) {
                if (!fileFormats.contains(s)) {
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
        asset.updateUser = user.username;
        rightsValidationService.checkWriteRights(user, asset.institution, asset.collection);
        valiedateAndSetCollectionId(asset);
        validateNewAssetAndSetIds(asset);
        validateAsset(asset);
        // Create share
        Optional<Collection> collectionInternal = collectionService.findCollectionInternal(asset.collection, asset.institution);
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
            if (!Strings.isNullOrEmpty(asset.digitiser)) {
                User user1 = userService.ensureExists(new User(asset.digitiser));
                asset.digitiser_id = user1.dassco_user_id;
            }

            // Create the asset
//            Observation.createNotStarted("persist:create-asset", observationRegistry).observe(() -> {
//                jdbi.onDemand(AssetRepository.class)
//                        .createAsset(asset);
//            });
            AssetRepository assetRepository = h.attach(AssetRepository.class);
            EventRepository eventRepository = h.attach(EventRepository.class);
            UserRepository userRepository = h.attach(UserRepository.class);
            FundingRepository fundingRepository = h.attach(FundingRepository.class);
            SpecimenRepository specimenRepository = h.attach(SpecimenRepository.class);
//            List<Specimen> specimensByAsset = specimenRepository.findSpecimensByAsset(asset.asset_guid);
//            Map<String, Specimen> pidSpecimen = new HashMap<>();
//            for(Specimen specimen: specimensByAsset){
//                pidSpecimen.put(specimen.specimen_pid(), specimen);
//            }
//

            assetRepository.insertBaseAsset(asset);
            // Handle funds
            for (String funding : asset.funding) {
                Funding funding1 = fundingService.ensureExists(funding);
                fundingRepository.fundAsset(asset.asset_guid, funding1.funding_id());
            }
            // Handle digitiser_list
            for (String s : asset.complete_digitiser_list) {
                User user1 = userService.ensureExists(new User(s));
                userRepository.addDigitiser(asset.asset_guid, user1.dassco_user_id);
            }
            // Handle specimen
            for (Specimen specimen : asset.specimens) {
                Optional<Specimen> specimensByPID = specimenRepository.findSpecimensByPID(specimen.specimen_pid());
                if (specimensByPID.isEmpty()) {
                    Specimen specimenToPersist = new Specimen(asset.institution, asset.collection, specimen.barcode(), specimen.specimen_pid(), specimen.preparation_type(), specimen.specimen_id(), asset.collection_id);
                    specimenRepository.insert_specimen(specimenToPersist);
                    Optional<Specimen> newSpecimenOpt = specimenRepository.findSpecimensByPID(specimenToPersist.specimen_pid());
                    Specimen newSpecimen = newSpecimenOpt.orElseThrow(() -> new RuntimeException("This shouldn't happen"));
                    specimenRepository.attachSpecimen(asset.asset_guid, newSpecimen.specimen_id());
                } else {
                    Specimen existing = specimensByPID.get();
                    if (!existing.barcode().equals(specimen.barcode()) || !existing.preparation_type().equals(specimen.preparation_type())) {
                        Specimen updated = new Specimen(asset.institution, asset.collection, specimen.barcode(), existing.specimen_pid(), specimen.preparation_type(), existing.specimen_id(), asset.collection_id);
                        specimenRepository.updateSpecimen(updated);
                    }
                    specimenRepository.attachSpecimen(asset.asset_guid, existing.specimen_id());
                }

            }
            Integer pipelineId = null;
            if (asset.pipeline != null) {
                Optional<Pipeline> pipelineByInstitutionAndName = pipelineService.findPipelineByInstitutionAndName(asset.pipeline, asset.institution);
                if (pipelineByInstitutionAndName.isPresent()) {
                    pipelineId = pipelineByInstitutionAndName.get().pipeline_id();
                }
            }
            System.out.println("PYPELINE ID " + pipelineId);
            eventRepository.insertEvent(asset.asset_guid, DasscoEvent.CREATE_ASSET_METADATA, user.dassco_user_id, pipelineId);
//            LocalDateTime createAssetEnd = LocalDateTime.now();
//            logger.info("#5 Creating the asset took {} ms", Duration.between(createAssetStart, createAssetEnd).toMillis());
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


//        statisticsDataServiceV2.refreshCachedData();
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
            return specimenRepository.listPreparationTypesInternal();
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
        valiedateAndSetCollectionId(updatedAsset);
        Asset existing = assetOpt.get();
        rightsValidationService.checkWriteRightsThrowing(user, existing.institution, existing.collection);
        Set<String> updatedSpecimenBarcodes = updatedAsset.specimens.stream().map(Specimen::barcode).collect(Collectors.toSet());

        List<Specimen> specimensToDetach = existing.specimens.stream().filter(s -> !updatedSpecimenBarcodes.contains(s.barcode())).collect(Collectors.toList());
        existing.collection_id = updatedAsset.collection_id;
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

        // Currently we just add new subject types if they do not exist
        if (!Strings.isNullOrEmpty(existing.subject) && !extendableEnumService.getSubjects().contains(existing.subject)) {
            extendableEnumService.persistEnum(ExtendableEnumService.ExtendableEnum.SUBJECT, existing.subject);
        }
        validateAssetFields(existing);
        jdbi.inTransaction(h -> {
            AssetRepository repository = h.attach(AssetRepository.class);
            EventRepository eventRepository = h.attach(EventRepository.class);
            SpecimenRepository specimenRepository = h.attach(SpecimenRepository.class);
            repository.update_asset_internal(existing);
            Optional<Pipeline> pipelineByInstitutionAndName = pipelineService.findPipelineByInstitutionAndName(existing.institution, updatedAsset.pipeline);
            eventRepository.insertEvent(existing.asset_guid
                    , DasscoEvent.UPDATE_ASSET_METADATA
                    , user.dassco_user_id
                    , pipelineByInstitutionAndName.isPresent() ? pipelineByInstitutionAndName.get().pipeline_id() : null);
            for (Specimen s : specimensToDetach) {
                specimenRepository.detachSpecimen(existing.asset_guid, s.specimen_id());
            }
            for (Specimen s : existing.specimens) {
                Optional<Specimen> specimensByPID = specimenRepository.findSpecimensByPID(s.specimen_pid());
                if (specimensByPID.isEmpty()) {
                    Specimen newSpecimen = new Specimen(existing.institution, existing.collection, s.barcode(), s.specimen_pid(), s.preparation_type(), s.specimen_id(), existing.collection_id);
                    Integer specimen_id = specimenRepository.insert_specimen(newSpecimen);
                    specimenRepository.attachSpecimen(updatedAsset.asset_guid, specimen_id);
                } else {
                    Specimen updated = new Specimen(existing.institution, existing.collection, s.barcode(), s.specimen_pid(), s.preparation_type(), specimensByPID.get().specimen_id(), existing.collection_id);
                    specimenRepository.updateSpecimen(updated);
                }

            }

            return h;
        });


        //TODO fix queries
//        statisticsDataServiceV2.refreshCachedData();

        logger.info("Adding Digitiser to Cache if absent in Update Asset Method");
//        digitiserCache.putDigitiserInCacheIfAbsent(new Digitiser(updatedAsset.digitiser, updatedAsset.digitiser));


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

//        boolean prepTypeExists = true;
//        if (updatedAsset.specimens != null && !updatedAsset.specimens.isEmpty()) {
//            for (Specimen specimen : updatedAsset.specimens) {
//                if (!preparationTypeCache.getPreparationTypeMap().containsKey(specimen.preparation_type())) {
//                    prepTypeExists = false;
//                    break;
//                }
//            }
//            if (!prepTypeExists) {
//                preparationTypeCache.clearCache();
//                List<String> preparationTypeList = jdbi.withHandle(handle -> {
//                    SpecimenRepository specimenRepository = handle.attach(SpecimenRepository.class);
//                    return specimenRepository.listPreparationTypesInternal();
//                });
//                if (!preparationTypeList.isEmpty()) {
//                    for (String preparationType : preparationTypeList) {
//                        this.preparationTypeCache.putPreparationTypesInCacheIfAbsent(preparationType);
//                    }
//                }
//            }
//        }
        return existing;
    }

    public void setIds(Asset asset) {
        Integer pipelineId = null;
        if (asset.pipeline != null) {
            Optional<Pipeline> pipelineByInstitutionAndName = pipelineService.findPipelineByInstitutionAndName(asset.pipeline, asset.institution);
            if (pipelineByInstitutionAndName.isPresent()) {
                pipelineId = pipelineByInstitutionAndName.get().pipeline_id();
            }
        }
        asset.updating_pipeline_id = pipelineId;
        Optional<Collection> collectionInternal = collectionService.findCollectionInternal(asset.collection, asset.institution);
        collectionInternal.ifPresent(collection -> asset.collection_id = collection.collection_id());

    }

    public boolean completeAsset(AssetUpdateRequest assetUpdateRequest, User user) {
        Optional<Asset> optAsset = getAsset(assetUpdateRequest.minimalAsset().asset_guid());
        if (optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }
        Asset asset = optAsset.get();
        asset.internal_status = InternalStatus.COMPLETED;
        asset.error_message = null;
        asset.error_timestamp = null;
        Optional<Pipeline> optPipl = pipelineService.findPipelineByInstitutionAndName(assetUpdateRequest.pipeline(), asset.institution);

        Event event = new Event(assetUpdateRequest.digitiser(), Instant.now(), DasscoEvent.CREATE_ASSET, assetUpdateRequest.pipeline());

        jdbi.withHandle(h -> {
            AssetRepository assetRepository = h.attach(AssetRepository.class);
            EventRepository eventRepository = h.attach(EventRepository.class);
            assetRepository.updateAssetStatus(asset);
            eventRepository.insertEvent(asset.asset_guid
                    , DasscoEvent.CREATE_ASSET
                    , user.dassco_user_id
                    , optPipl.map(Pipeline::pipeline_id).orElse(null));
            return h;
        });

        //TODO fix stats
//        statisticsDataServiceV2.refreshCachedData();


//        if (assetUpdateRequest.digitiser() != null && !assetUpdateRequest.digitiser().isEmpty()) {
//            logger.info("Adding Digitiser to Cache if absent in Complete Asset Method");
//            digitiserCache.putDigitiserInCacheIfAbsent(new Digitiser(assetUpdateRequest.digitiser(), assetUpdateRequest.digitiser()));
//        }
        return true;
    }

    public boolean auditAsset(User user, Audit audit, String assetGuid) {
        Optional<Asset> optAsset = getAsset(assetGuid);
        if (Strings.isNullOrEmpty(audit.user())) {
            throw new IllegalArgumentException("Audit must have a user!");
        }
        if (optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }
        Asset asset = optAsset.get();
        rightsValidationService.checkReadRightsThrowing(user, asset.institution, asset.collection);
        if (!InternalStatus.COMPLETED.equals(asset.internal_status)) {
            throw new DasscoIllegalActionException("Asset must be complete before auditing");
        }
        if (Objects.equals(asset.digitiser, audit.user())) {
            throw new DasscoIllegalActionException("Audit cannot be performed by the user who digitized the asset");
        }
        Event event = new Event(audit.user(), Instant.now(), DasscoEvent.AUDIT_ASSET, null);
        jdbi.onDemand(EventRepository.class).insertEvent(asset.asset_guid, DasscoEvent.AUDIT_ASSET, user.dassco_user_id, null);

        logger.info("Adding Digitiser to Cache if absent in Audit Asset Method");
        digitiserCache.putDigitiserInCacheIfAbsent(new Digitiser(audit.user(), audit.user()));

        return true;
    }

    public List<Event> getEvents(String assetGuid, User user) {
        //TODO find a way to check rights witout loading entire asset
        Optional<Asset> assetOpt = this.getAsset(assetGuid);
        if (assetOpt.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist");
        }
        Asset asset = assetOpt.get();
        rightsValidationService.checkReadRightsThrowing(user, asset.institution, asset.collection);
        return jdbi.onDemand(EventRepository.class).getAssetEvents(assetGuid);
    }


    // The upload of files to file proxy is completed. The asset is now awaiting ERDA synchronization.
    public boolean completeUpload(AssetUpdateRequest assetSmbRequest, User user) {
        if (assetSmbRequest.minimalAsset() == null) {
            throw new IllegalArgumentException("Asset cannot be null");
        }
        Optional<Asset> optAsset = getAsset(assetSmbRequest.minimalAsset().asset_guid());
        if (optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }

        // Mark as asset received
        Asset asset = optAsset.get();
        rightsValidationService.checkWriteRights(user, asset.institution, asset.collection);
        if (asset.asset_locked) {
            throw new DasscoIllegalActionException("Asset is locked");
        }
        asset.internal_status = InternalStatus.ASSET_RECEIVED;
        // Close samba and sync ERDA;
        jdbi.onDemand(AssetRepository.class).updateAssetNoEvent(asset);
        statisticsDataServiceV2.refreshCachedData();

        logger.info("Adding Digitiser to Cache if absent in Complete Upload Asset Method");
        digitiserCache.putDigitiserInCacheIfAbsent(new Digitiser(user.username, user.username));
        return true;
    }

    public boolean setAssetStatus(String assetGuid, String status, String errorMessage) {
        InternalStatus assetStatus = null;
        try {
            assetStatus = InternalStatus.valueOf(status);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        if (assetStatus != InternalStatus.ERDA_ERROR && assetStatus != InternalStatus.ASSET_RECEIVED) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        Optional<Asset> optAsset = getAsset(assetGuid);
        if (optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }
        Asset asset = optAsset.get();
        asset.internal_status = assetStatus;
        asset.error_message = errorMessage;
        if (!InternalStatus.ASSET_RECEIVED.equals(asset.internal_status)) {
            asset.error_timestamp = Instant.now();
        }
        jdbi.onDemand(AssetRepository.class)
                .updateAssetNoEvent(asset);

        statisticsDataServiceV2.refreshCachedData();
        return true;
    }

    public void deleteAssetMetadata(String assetGuid, User user) {
        // Check that the asset exists:
        Optional<Asset> optAsset = getAsset(assetGuid);
        if (optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }

        // Close the share if open:
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fileProxyConfiguration.url() + "/shares/assets/" + assetGuid + "/deleteShare"))
                .header("Authorization", "Bearer " + user.token)
                .DELETE()
                .build();

        HttpClient httpClient = createHttpClient();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 || response.statusCode() == 404) {
                Asset asset = optAsset.get();
                rightsValidationService.checkWriteRightsThrowing(user, asset.institution, asset.collection);
                jdbi.onDemand(AssetRepository.class).deleteAsset(assetGuid);

                // Refresh cache:
                reloadAssetCache();
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    //TODO pipeline
    public boolean deleteAsset(String assetGuid, User user) {
        String userId = user.username;
        if (Strings.isNullOrEmpty(userId)) {
            throw new IllegalArgumentException("User is null");
        }
        Optional<Asset> optAsset = getAsset(assetGuid);
        if (optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }
        Asset asset = optAsset.get();
        rightsValidationService.checkReadRights(user, asset.institution, asset.collection);
        if (asset.asset_locked) {
            throw new DasscoIllegalActionException("Asset is locked");
        }
        if (asset.date_asset_deleted != null) {
            throw new IllegalArgumentException("Asset is already deleted");
        }

        Event event = new Event(userId, Instant.now(), DasscoEvent.DELETE_ASSET_METADATA, null);
        jdbi.onDemand(EventRepository.class).insertEvent(asset.asset_guid, DasscoEvent.DELETE_ASSET_METADATA, user.dassco_user_id, null);

        statisticsDataServiceV2.refreshCachedData();

        logger.info("Adding Digitiser to Cache if absent in Delete Asset Method");
        digitiserCache.putDigitiserInCacheIfAbsent(new Digitiser(user.username, user.username));

        return true;
    }

    public boolean unlockAsset(String assetGuid) {
        Optional<Asset> optAsset = getAsset(assetGuid);
        if (optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }
        Asset asset = optAsset.get();
        asset.asset_locked = false;
        jdbi.onDemand(AssetRepository.class).updateAssetNoEvent(asset);
        return true;
    }

    public HttpClient createHttpClient() {
        return HttpClient.newHttpClient();
    }

}
