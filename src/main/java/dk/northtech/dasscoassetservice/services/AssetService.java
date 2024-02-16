package dk.northtech.dasscoassetservice.services;

import com.google.common.base.Strings;
import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.repositories.AssetRepository;
import dk.northtech.dasscoassetservice.webapi.domain.HttpAllocationStatus;
import dk.northtech.dasscoassetservice.webapi.domain.SambaRequestStatus;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AssetService {
    private final InstitutionService institutionService;
    private final CollectionService collectionService;
    private final WorkstationService workstationService;
    private final StatisticsDataService statisticsDataService;
    private final FileProxyClient fileProxyClient;
    private final PipelineService pipelineService;
    private final Jdbi jdbi;

    @Inject
    public AssetService(InstitutionService institutionService, CollectionService collectionService, WorkstationService workstationService, @Lazy FileProxyClient fileProxyClient, Jdbi jdbi, StatisticsDataService statisticsDataService, PipelineService pipelineService) {
        this.institutionService = institutionService;
        this.collectionService = collectionService;
        this.workstationService = workstationService;
        this.fileProxyClient = fileProxyClient;
        this.statisticsDataService = statisticsDataService;
        this.pipelineService = pipelineService;
        this.jdbi = jdbi;
    }

    public boolean auditAsset(Audit audit, String assetGuid) {
        Optional<Asset> optAsset = getAsset(assetGuid);
        if(Strings.isNullOrEmpty(audit.user())) {
            throw new IllegalArgumentException("Audit must have a user!");
        }
        if(optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }
        Asset asset = optAsset.get();
        if(!InternalStatus.COMPLETED.equals(asset.internal_status)){
            throw new DasscoIllegalActionException("Asset must be complete before auditing");
        }
        if(Objects.equals(asset.digitiser, audit.user())) {
            throw new DasscoIllegalActionException("Audit cannot be performed by the user who digitized the asset");
        }
        Event event = new Event(audit.user(), Instant.now(), DasscoEvent.AUDIT_ASSET, null, null);
        jdbi.onDemand(AssetRepository.class).setEvent(audit.user(), event,asset);
        return true;
    }

    public boolean deleteAsset(String assetGuid, User user) {
        String userId = user.username;
        if(Strings.isNullOrEmpty(userId)) {
            throw new IllegalArgumentException("User is null");
        }
        Optional<Asset> optAsset = getAsset(assetGuid);
        if(optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }
        Asset asset = optAsset.get();
        if(asset.asset_locked) {
            throw new DasscoIllegalActionException("Asset is locked");
        }
        if(asset.date_asset_deleted != null) {
            throw new IllegalArgumentException("Asset is already deleted");
        }
        Event event = new Event(userId, Instant.now(), DasscoEvent.DELETE_ASSET_METADATA, null, null);
        jdbi.onDemand(AssetRepository.class).setEvent(userId, event, asset);
        return true;
    }

    public boolean unlockAsset(String assetGuid) {
        Optional<Asset> optAsset = getAsset(assetGuid);
        if(optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }
        Asset asset = optAsset.get();
        asset.asset_locked = false;
        jdbi.onDemand(AssetRepository.class).updateAssetNoEvent(asset);
        return true;
    }
    public List<Event> getEvents(String assetGuid) {
        return jdbi.onDemand(AssetRepository.class).readEvents(assetGuid);
    }
    public boolean completeAsset(AssetUpdateRequest assetUpdateRequest) {
        Optional<Asset> optAsset = getAsset(assetUpdateRequest.minimalAsset().asset_guid());
        if(optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }
        Asset asset = optAsset.get();
        asset.internal_status = InternalStatus.COMPLETED;
        Event event = new Event(assetUpdateRequest.digitiser(), Instant.now(),DasscoEvent.CREATE_ASSET, assetUpdateRequest.pipeline(), assetUpdateRequest.workstation());
        jdbi.onDemand(AssetRepository.class).updateAssetAndEvent(asset,event);
        return true;
    }

    public boolean completeUpload(AssetUpdateRequest assetSmbRequest, User user) {
        if(assetSmbRequest.minimalAsset() == null) {
            throw new IllegalArgumentException("Asset cannot be null");
        }
        if(assetSmbRequest.shareName() == null) {
            throw new IllegalArgumentException("Share id cannot be null");
        }
        Optional<Asset> optAsset = getAsset(assetSmbRequest.minimalAsset().asset_guid());
        if(optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }
        //Mark as asset received
        Asset asset = optAsset.get();
        if(asset.asset_locked) {
            throw new DasscoIllegalActionException("Asset is locked");
        }
        asset.internal_status = InternalStatus.ASSET_RECEIVED;
        // Close samba and sync ERDA
        // If media is successfully moved to ERDA fileproxy will contact assetService and set status to completed.
        fileProxyClient.closeSamba(user, assetSmbRequest, true);
        jdbi.onDemand(AssetRepository.class).updateAssetNoEvent(asset);
        return true;
    }

    public boolean setFailedStatus(String assetGuid, String status) {
        InternalStatus assetStatus = null;
        try {
            assetStatus = InternalStatus.valueOf(status);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        if(assetStatus != InternalStatus.ERDA_ERROR && assetStatus != InternalStatus.ERDA_FAILED) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        Optional<Asset> optAsset = getAsset(assetGuid);
        if(optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }
        Asset asset = optAsset.get();
        asset.internal_status = assetStatus;
        jdbi.onDemand(AssetRepository.class)
                .updateAssetNoEvent(asset);
        return true;
    }

    public Asset updateAsset(Asset updatedAsset) {
        Optional<Asset> assetOpt = getAsset(updatedAsset.asset_guid);
        if(assetOpt.isEmpty()) {
            throw new IllegalArgumentException("Asset " + updatedAsset.asset_guid + " does not exist");
        }
        if(Strings.isNullOrEmpty(updatedAsset.updateUser)) {
            throw new IllegalArgumentException("Update user must be provided");
        }
        validateAsset(updatedAsset);
        Asset existing = assetOpt.get();
        Set<String> updatedSpecimenBarcodes = updatedAsset.specimens.stream().map(Specimen::barcode).collect(Collectors.toSet());

        List<Specimen> specimensToDetach = existing.specimens.stream().filter(s -> !updatedSpecimenBarcodes.contains(s.barcode())).collect(Collectors.toList());
        existing.specimens = updatedAsset.specimens;
        existing.tags = updatedAsset.tags;
        existing.workstation= updatedAsset.workstation;
        existing.pipeline = updatedAsset.pipeline;
        existing.date_asset_finalised = updatedAsset.date_asset_finalised;
        existing.status = updatedAsset.status;
        if(existing.asset_locked && !updatedAsset.asset_locked) {
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
        return existing;
    }

    void validateAssetFields(Asset a) {
        if(Strings.isNullOrEmpty(a.asset_guid)) {
            throw new IllegalArgumentException("asset_guid cannot be null");
        }
        if(Strings.isNullOrEmpty(a.asset_pid)){
            throw new IllegalArgumentException("asset_pid cannot be null");
        }
        if(a.status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
    }

    void validateAsset(Asset asset){
        Optional<Institution> ifExists = institutionService.getIfExists(asset.institution);
        if(ifExists.isEmpty()){
            throw new IllegalArgumentException("Institution doesnt exist");
        }
        Optional<Collection> collectionOpt = collectionService.findCollection(asset.collection);
        if(collectionOpt.isEmpty()) {
            throw new IllegalArgumentException("Collection doesnt exist");
        }
        Optional<Pipeline> pipelineOpt = pipelineService.findPipelineByInstitutionAndName(asset.pipeline, asset.institution);
        if(pipelineOpt.isEmpty()) {
            throw new IllegalArgumentException("Pipeline doesnt exist in this institution");
        }
        if(asset.asset_guid.equals(asset.parent_guid)) {
            throw new IllegalArgumentException("Asset cannot be its own parent");
        }
        Optional<Workstation> workstationOpt = workstationService.findWorkstation(asset.workstation);
        if(workstationOpt.isEmpty()){
            throw new IllegalArgumentException("Workstation does not exist");
        }
        Workstation workstation = workstationOpt.get();
        if(workstation.status().equals(WorkstationStatus.OUT_OF_SERVICE)){
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
        if(assetOpt.isPresent()) {
            throw new IllegalArgumentException("Asset " + asset.asset_guid + " already exists");
        }
        validateAssetFields(asset);
        validateAsset(asset);
        //TODO
        asset.httpInfo = fileProxyClient.openHttpShare(new MinimalAsset(asset.asset_guid, asset.parent_guid, asset.institution, asset.collection), user, allocation);
        // Default values on creation
        asset.date_metadata_updated = Instant.now();
        asset.created_date = Instant.now();
        asset.internal_status = InternalStatus.METADATA_RECEIVED;

        if(asset.httpInfo.httpAllocationStatus() == HttpAllocationStatus.SUCCESS) {
            jdbi.onDemand(AssetRepository.class)
                    .createAsset(asset);
        } else {
            //Do not persist azzet if share wasnt created
            return asset;
        }
        this.statisticsDataService.addAssetToCache(asset);
        return asset;
    }

    public Optional<Asset> getAsset(String assetGuid) {
        return jdbi.onDemand(AssetRepository.class).readAsset(assetGuid);
    }

}
