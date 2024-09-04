package dk.northtech.dasscoassetservice.services;

import com.google.common.base.Strings;
import dk.northtech.dasscoassetservice.cache.*;
import dk.northtech.dasscoassetservice.configuration.FileProxyConfiguration;
import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.repositories.AssetRepository;
import dk.northtech.dasscoassetservice.repositories.SpecimenRepository;
import dk.northtech.dasscoassetservice.webapi.domain.HttpAllocationStatus;
import dk.northtech.dasscoassetservice.webapi.domain.HttpInfo;
import jakarta.inject.Inject;
import org.apache.age.jdbc.base.type.AgtypeListBuilder;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssetService {
    private final InstitutionService institutionService;
    private final CollectionService collectionService;
    private final WorkstationService workstationService;
//    private final StatisticsDataService statisticsDataService;
    private final StatisticsDataServiceV2 statisticsDataServiceV2;
    private final FileProxyClient fileProxyClient;
    private final PipelineService pipelineService;
    private final RightsValidationService rightsValidationService;
    private final Jdbi jdbi;
    private final DigitiserCache digitiserCache;
    private final SubjectCache subjectCache;
    private final PayloadTypeCache payloadTypeCache;
    private final StatusCache statusCache;
    private final PreparationTypeCache preparationTypeCache;
    private final RestrictedAccessCache restrictedAccessCache;
    private static final Logger logger = LoggerFactory.getLogger(AssetService.class);
    private final FileProxyConfiguration fileProxyConfiguration;

    @Inject
    public AssetService(InstitutionService institutionService
            , CollectionService collectionService
            , WorkstationService workstationService
            , @Lazy FileProxyClient fileProxyClient
            , Jdbi jdbi
            , StatisticsDataServiceV2 statisticsDataServiceV2
            , PipelineService pipelineService
            , RightsValidationService rightsValidationService,
                        DigitiserCache digitiserCache,
                        SubjectCache subjectCache,
                        PayloadTypeCache payloadTypeCache,
                        StatusCache statusCache,
                        PreparationTypeCache preparationTypeCache,
                        RestrictedAccessCache restrictedAccessCache,
                        FileProxyConfiguration fileProxyConfiguration) {
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
        this.statusCache = statusCache;
        this.rightsValidationService = rightsValidationService;
        this.preparationTypeCache = preparationTypeCache;
        this.restrictedAccessCache = restrictedAccessCache;
        this.fileProxyConfiguration = fileProxyConfiguration;
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
        Event event = new Event(audit.user(), Instant.now(), DasscoEvent.AUDIT_ASSET, null, null);
        jdbi.onDemand(AssetRepository.class).setEvent(audit.user(), event, asset);

        logger.info("Adding Digitiser to Cache if absent in Audit Asset Method");
        digitiserCache.putDigitiserInCacheIfAbsent(new Digitiser(audit.user(), audit.user()));

        return true;
    }

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

        Event event = new Event(userId, Instant.now(), DasscoEvent.DELETE_ASSET_METADATA, null, null);
        jdbi.onDemand(AssetRepository.class).setEvent(userId, event, asset);

        statisticsDataServiceV2.refreshCachedData();

        logger.info("Adding Digitiser to Cache if absent in Delete Asset Method");
        digitiserCache.putDigitiserInCacheIfAbsent(new Digitiser(user.username, user.username));

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

        HttpClient httpClient = HttpClient.newHttpClient();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 || response.statusCode() == 404){
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

    public List<Event> getEvents(String assetGuid, User user) {
        Optional<Asset> assetOpt = this.getAsset(assetGuid);
        if (assetOpt.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist");
        }
        Asset asset = assetOpt.get();
        rightsValidationService.checkReadRightsThrowing(user, asset.institution, asset.collection);
        return jdbi.onDemand(AssetRepository.class).readEvents(assetGuid);
    }

    public boolean completeAsset(AssetUpdateRequest assetUpdateRequest) {
        Optional<Asset> optAsset = getAsset(assetUpdateRequest.minimalAsset().asset_guid());
        if (optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }
        Asset asset = optAsset.get();
        asset.internal_status = InternalStatus.COMPLETED;
        asset.error_message = null;
        asset.error_timestamp = null;
        Event event = new Event(assetUpdateRequest.digitiser(), Instant.now(), DasscoEvent.CREATE_ASSET, assetUpdateRequest.pipeline(), assetUpdateRequest.workstation());
        jdbi.onDemand(AssetRepository.class).updateAssetAndEvent(asset, event);

        statisticsDataServiceV2.refreshCachedData();


        if (assetUpdateRequest.digitiser() != null && !assetUpdateRequest.digitiser().isEmpty()){
            logger.info("Adding Digitiser to Cache if absent in Complete Asset Method");
            digitiserCache.putDigitiserInCacheIfAbsent(new Digitiser(assetUpdateRequest.digitiser(), assetUpdateRequest.digitiser()));
        }



        return true;
    }

    public boolean completeUpload(AssetUpdateRequest assetSmbRequest, User user) {
        if (assetSmbRequest.minimalAsset() == null) {
            throw new IllegalArgumentException("Asset cannot be null");
        }
        if (assetSmbRequest.shareName() == null) {
            throw new IllegalArgumentException("Share id cannot be null");
        }
        Optional<Asset> optAsset = getAsset(assetSmbRequest.minimalAsset().asset_guid());
        if (optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }

        //Mark as asset received
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
        if (assetStatus != InternalStatus.ERDA_ERROR && assetStatus != InternalStatus.ERDA_FAILED && assetStatus != InternalStatus.ASSET_RECEIVED) {
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
        existing.funding = updatedAsset.funding;
        existing.file_formats = updatedAsset.file_formats;
        existing.payload_type = updatedAsset.payload_type;
        existing.digitiser = updatedAsset.digitiser;
        existing.parent_guid = updatedAsset.parent_guid;
        existing.updateUser = updatedAsset.updateUser;
        existing.asset_pid = updatedAsset.asset_pid == null ? existing.asset_pid : updatedAsset.asset_pid;
        validateAssetFields(existing);
        jdbi.onDemand(AssetRepository.class).updateAsset(existing, specimensToDetach);

        statisticsDataServiceV2.refreshCachedData();

        logger.info("Adding Digitiser to Cache if absent in Update Asset Method");
        digitiserCache.putDigitiserInCacheIfAbsent(new Digitiser(updatedAsset.updateUser, updatedAsset.updateUser));


        if (updatedAsset.subject != null && !updatedAsset.subject.isEmpty()){
            if (!subjectCache.getSubjectMap().containsKey(updatedAsset.subject)){
                this.subjectCache.clearCache();
                List<String> subjectList = jdbi.withHandle(handle -> {
                    AssetRepository assetRepository = handle.attach(AssetRepository.class);
                    return assetRepository.listSubjects();
                });
                if (!subjectList.isEmpty()){
                    for (String subject : subjectList){
                        this.subjectCache.putSubjectsInCacheIfAbsent(subject);
                    }
                }
            }
        }

        if (updatedAsset.payload_type != null && !updatedAsset.payload_type.isEmpty()){
            if (!payloadTypeCache.getPayloadTypeMap().containsKey(updatedAsset.payload_type)){
                this.payloadTypeCache.clearCache();
                List<String> payloadTypeList = jdbi.withHandle(handle -> {
                    AssetRepository assetRepository = handle.attach(AssetRepository.class);
                    return assetRepository.listPayloadTypes();
                });
                if (!payloadTypeList.isEmpty()){
                    for (String payloadType : payloadTypeList){
                        this.payloadTypeCache.putPayloadTypesInCacheIfAbsent(payloadType);
                    }
                }
            }
        }

        if (updatedAsset.status != null && !updatedAsset.status.toString().isEmpty()){
            if (!statusCache.getStatusMap().containsKey(updatedAsset.status.toString())){
                statusCache.clearCache();
                List<AssetStatus> statusList = jdbi.withHandle(handle -> {
                    AssetRepository assetRepository = handle.attach(AssetRepository.class);
                    return assetRepository.listStatus();
                });
                if (!statusList.isEmpty()){
                    for(AssetStatus status : statusList){
                        this.statusCache.putStatusInCacheIfAbsent(status);
                    }
                }
            }
        }

        boolean prepTypeExists = true;
        if (updatedAsset.specimens != null && !updatedAsset.specimens.isEmpty()){
            for (Specimen specimen : updatedAsset.specimens){
                if (!preparationTypeCache.getPreparationTypeMap().containsKey(specimen.preparation_type())){
                    prepTypeExists = false;
                }
            }
            if(!prepTypeExists){
                preparationTypeCache.clearCache();
                List<String> preparationTypeList = jdbi.withHandle(handle -> {
                    SpecimenRepository specimenRepository = handle.attach(SpecimenRepository.class);
                    return specimenRepository.listPreparationTypes();
                });
                if (!preparationTypeList.isEmpty()){
                    for (String preparationType : preparationTypeList){
                        this.preparationTypeCache.putPreparationTypesInCacheIfAbsent(preparationType);
                    }
                }
            }
        }

        if (updatedAsset.restricted_access != null && !updatedAsset.restricted_access.isEmpty()){
            boolean restrictedAccessExists = true;
            for (InternalRole internalRole : updatedAsset.restricted_access){
                if (!restrictedAccessCache.getRestrictedAccessMap().containsKey(internalRole.toString())){
                    restrictedAccessExists = false;
                }
            }
            if (!restrictedAccessExists){
                restrictedAccessCache.clearCache();
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
        }

        return existing;
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
        List<Asset> assets = jdbi.onDemand(AssetRepository.class).readMultipleAssets(assetList);

        for (Asset asset : assets) {
            rightsValidationService.checkWriteRightsThrowing(user, asset.institution, asset.collection);
        }

        if (assets.size() != assetList.size()) {
            throw new IllegalArgumentException("One or more assets were not found!");
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

        // Create the new BULK_UPDATE_ASSET_METADATA event:
        Event event = new Event();
        event.event = DasscoEvent.BULK_UPDATE_ASSET_METADATA;
        event.user = updatedAsset.updateUser;
        event.workstation = updatedAsset.workstation;
        event.pipeline = updatedAsset.pipeline;
        event.timeStamp = Instant.now();

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

        List<Asset> bulkUpdateSuccess = jdbi.onDemand(AssetRepository.class).bulkUpdate(sql, builder, updatedAsset, event, assets, assetList);

        logger.info("Adding Digitiser to Cache if absent in Bulk Update Asset Method");
        digitiserCache.putDigitiserInCacheIfAbsent(new Digitiser(updatedAsset.updateUser, updatedAsset.updateUser));

        if (updatedAsset.subject != null && !updatedAsset.subject.isEmpty()){
            if (!subjectCache.getSubjectMap().containsKey(updatedAsset.subject)){
                this.subjectCache.clearCache();
                List<String> subjectList = jdbi.withHandle(handle -> {
                    AssetRepository assetRepository = handle.attach(AssetRepository.class);
                    return assetRepository.listSubjects();
                });
                if (!subjectList.isEmpty()){
                    for (String subject : subjectList){
                        this.subjectCache.putSubjectsInCacheIfAbsent(subject);
                    }
                }
            }
        }

        if (updatedAsset.payload_type != null && !updatedAsset.payload_type.isEmpty()){
            if (!payloadTypeCache.getPayloadTypeMap().containsKey(updatedAsset.payload_type)){
                this.payloadTypeCache.clearCache();
                List<String> payloadTypeList = jdbi.withHandle(handle -> {
                    AssetRepository assetRepository = handle.attach(AssetRepository.class);
                    return assetRepository.listPayloadTypes();
                });
                if (!payloadTypeList.isEmpty()){
                    for (String payloadType : payloadTypeList){
                        this.payloadTypeCache.putPayloadTypesInCacheIfAbsent(payloadType);
                    }
                }
            }
        }

        if (updatedAsset.status != null && !updatedAsset.status.toString().isEmpty()){
            if (!statusCache.getStatusMap().containsKey(updatedAsset.status.toString())){
                statusCache.clearCache();
                List<AssetStatus> statusList = jdbi.withHandle(handle -> {
                    AssetRepository assetRepository = handle.attach(AssetRepository.class);
                    return assetRepository.listStatus();
                });
                if (!statusList.isEmpty()){
                    for(AssetStatus status : statusList){
                        this.statusCache.putStatusInCacheIfAbsent(status);
                    }
                }
            }
        }

        return bulkUpdateSuccess;
    }

    AgtypeMapBuilder bulkUpdateBuilderFactory(Asset updatedFields) {
        AgtypeMapBuilder builder = new AgtypeMapBuilder();

        if (updatedFields.status != null) {
            builder.add("status", updatedFields.status.name());
        }

        if (updatedFields.funding != null) {
            builder.add("funding", updatedFields.funding);
        }

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

    void validateAssetFields(Asset a) {
        if (Strings.isNullOrEmpty(a.asset_guid)) {
            throw new IllegalArgumentException("asset_guid cannot be null");
        }
        if (Strings.isNullOrEmpty(a.asset_pid)) {
            throw new IllegalArgumentException("asset_pid cannot be null");
        }
        if (a.status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
    }

    void validateAsset(Asset asset) {
        Optional<Institution> ifExists = institutionService.getIfExists(asset.institution);
        if (ifExists.isEmpty()) {
            throw new IllegalArgumentException("Institution doesnt exist");
        }
        Optional<Collection> collectionOpt = collectionService.findCollectionInternal(asset.collection, asset.institution);
        if (collectionOpt.isEmpty()) {
            throw new IllegalArgumentException("Collection doesnt exist");
        }
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

//        if(asset.parent_guid != null) {
//            Optional<Asset> parentOpt = getAsset(asset.parent_guid);
//            if(parentOpt.isEmpty()) {
//                throw new IllegalArgumentException("Parent doesnt exist");
//            }
//            Asset parent = parentOpt.get();
//            if(!parent.restricted_access.isEmpty()) {
//                parent.restricted_access.stream()
//                        .filter(role -> user.roles.contains(role.roleName))
//                        .findAny()
//                        .orElseThrow(() -> new DasscoIllegalActionException("Parent is restricted"));
//            }
//
//        }

    }


    public Asset persistAsset(Asset asset, User user, int allocation) {
        Optional<Asset> assetOpt = getAsset(asset.asset_guid);
        if (assetOpt.isPresent()) {
            throw new IllegalArgumentException("Asset " + asset.asset_guid + " already exists");
        }
        if (allocation == 0) {
            throw new IllegalArgumentException("Allocation cannot be 0");
        }
        rightsValidationService.checkWriteRights(user, asset.institution, asset.collection);
        validateAssetFields(asset);
        validateAsset(asset);

        asset.httpInfo = openHttpShare(new MinimalAsset(asset.asset_guid, asset.parent_guid, asset.institution, asset.collection), user, allocation);
        // Default values on creation
        asset.date_metadata_updated = Instant.now();
        asset.created_date = Instant.now();
        asset.internal_status = InternalStatus.METADATA_RECEIVED;

        if (asset.httpInfo.http_allocation_status() == HttpAllocationStatus.SUCCESS) {
            jdbi.onDemand(AssetRepository.class)
                    .createAsset(asset);

        statisticsDataServiceV2.refreshCachedData();

//        this.statisticsDataService.addAssetToCache(asset);


            if (asset.digitiser != null && !asset.digitiser.isEmpty()){
                logger.info("Adding Digitiser to Cache if absent in Persist Asset Method");
                digitiserCache.putDigitiserInCacheIfAbsent(new Digitiser(asset.digitiser, asset.digitiser));
            }

            if (asset.specimens != null && !asset.specimens.isEmpty()){
                for (Specimen specimen : asset.specimens){

                    preparationTypeCache.putPreparationTypesInCacheIfAbsent(specimen.preparation_type());

                }
            }

            if (asset.subject != null && !asset.subject.isEmpty()){

                subjectCache.putSubjectsInCacheIfAbsent(asset.subject);

            }

            if (asset.payload_type != null && !asset.payload_type.isEmpty()){
                    payloadTypeCache.putPayloadTypesInCacheIfAbsent(asset.payload_type);

            }

            statusCache.putStatusInCacheIfAbsent(asset.status);

            if (asset.restricted_access != null && !asset.restricted_access.isEmpty()){
                for (InternalRole internalRole : asset.restricted_access){
                    restrictedAccessCache.putRestrictedAccessInCacheIfAbsent(internalRole.toString());
                }
            }

        } else {
            //Do not persist azzet if share wasnt created
            return asset;
        }

        statisticsDataServiceV2.refreshCachedData();
//        this.statisticsDataService.addAssetToCache(asset);

        return asset;
    }

    public List<String> listSubjects(){
        return subjectCache.getSubjects();
    }

    public List<Digitiser> listDigitisers(){
        return digitiserCache.getDigitisers();
    }

    public List<String> listPayloadTypes(){
        return payloadTypeCache.getPayloadTypes();
    }

    public List<AssetStatus> listStatus(){
        return statusCache.getStatus();
    }

    public List<InternalRole>  listRestrictedAccess(){
        return restrictedAccessCache.getRestrictedAccessList();
    }

    public void reloadAssetCache(){
        subjectCache.clearCache();
        List<String> subjectList = jdbi.withHandle(handle -> {
            AssetRepository assetRepository = handle.attach(AssetRepository.class);
            return assetRepository.listSubjects();
        });
        if (!subjectList.isEmpty()){
            for (String subject : subjectList){
                this.subjectCache.putSubjectsInCacheIfAbsent(subject);
            }
        }
        payloadTypeCache.clearCache();
        List<String> payloadTypeList = jdbi.withHandle(handle -> {
            AssetRepository assetRepository = handle.attach(AssetRepository.class);
            return assetRepository.listPayloadTypes();
        });
        if (!payloadTypeList.isEmpty()){
            for (String payloadType : payloadTypeList){
                this.payloadTypeCache.putPayloadTypesInCacheIfAbsent(payloadType);
            }
        }
        preparationTypeCache.clearCache();
        List<String> preparationTypeList = jdbi.withHandle(handle -> {
            SpecimenRepository specimenRepository = handle.attach(SpecimenRepository.class);
            return specimenRepository.listPreparationTypes();
        });
        if (!preparationTypeList.isEmpty()){
            for (String preparationType : preparationTypeList){
                this.preparationTypeCache.putPreparationTypesInCacheIfAbsent(preparationType);
            }
        }
        statusCache.clearCache();
        List<AssetStatus> statusList = jdbi.withHandle(handle -> {
            AssetRepository assetRepository = handle.attach(AssetRepository.class);
            return assetRepository.listStatus();
        });
        if (!statusList.isEmpty()){
            for(AssetStatus status : statusList){
                this.statusCache.putStatusInCacheIfAbsent(status);
            }
        }
        restrictedAccessCache.clearCache();
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

    //This is here for mocking
    public HttpInfo openHttpShare(MinimalAsset minimalAsset, User updateUser, int allocation) {
        return fileProxyClient.openHttpShare(minimalAsset, updateUser, allocation);
    }

    public Optional<Asset> getAsset(String assetGuid) {
        return jdbi.onDemand(AssetRepository.class).readAsset(assetGuid);
    }

    public Optional<Asset> checkUserRights(String assetGuid, User user){
        Optional<Asset> optionalAsset = jdbi.onDemand(AssetRepository.class).readAsset(assetGuid);
        if (optionalAsset.isPresent()){
            Asset found = optionalAsset.get();
            rightsValidationService.checkReadRightsThrowing(user, found.institution, found.collection, found.asset_guid);
        }
        return optionalAsset;
    }

    public List<Asset> readMultipleAssets(List<String> assets){
        return jdbi.onDemand(AssetRepository.class).readMultipleAssets(assets);
    }

    public String createCSVString(List<Asset> assets){
        String csv = "";
        if (assets.isEmpty()){
            return "";
        }
        StringBuilder csvBuilder = new StringBuilder();
        Field[] fields = Asset.class.getDeclaredFields();
        String headers = String.join(",", Arrays.stream(fields).map(Field::getName).collect(Collectors.toList()));
        csvBuilder.append(headers).append("\n");
        for (Asset asset : assets){
            StringJoiner joiner = new StringJoiner(",");
            for (Field field : fields){
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

}
