package dk.northtech.dasscoassetservice.services;

import com.google.common.base.Strings;
import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.repositories.AssetRepository;
import dk.northtech.dasscoassetservice.webapi.domain.AssetSmbRequest;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class AssetService {
    private final InstitutionService institutionService;
    private final CollectionService collectionService;
    private final WorkstationService workstationService;
    private final StatisticsDataService statisticsDataService;
    private final FileProxyClient fileProxyClient;

    private final Jdbi jdbi;

    @Inject
    public AssetService(InstitutionService institutionService, CollectionService collectionService, WorkstationService workstationService, @Lazy FileProxyClient fileProxyClient, Jdbi jdbi, StatisticsDataService statisticsDataService) {
        this.institutionService = institutionService;
        this.collectionService = collectionService;
        this.workstationService = workstationService;
        this.fileProxyClient = fileProxyClient;
        this.statisticsDataService = statisticsDataService;
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
        if(Objects.equals(asset.digitizer, audit.user())) {
            throw new DasscoIllegalActionException("Audit cannot be performed by the user who digitized the asset");
        }
        jdbi.onDemand(AssetRepository.class).setEvent(audit.user(), DasscoEvent.AUDIT_ASSET, asset);
        return true;
    }

    public boolean deleteAsset(String user, String assetGuid) {
        Optional<Asset> optAsset = getAsset(assetGuid);
        if(Strings.isNullOrEmpty(user)) {
            throw new IllegalArgumentException("User is null");
        }
        if(optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }
        Asset asset = optAsset.get();
        if(asset.asset_deleted_date != null) {
            throw new IllegalArgumentException("Asset is already deleted");
        }
        jdbi.onDemand(AssetRepository.class).setEvent(user, DasscoEvent.DELETE_ASSET, asset);
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
    public boolean completeAsset(String assetGuid) {
        Optional<Asset> optAsset = getAsset(assetGuid);
        if(optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }
        Asset asset = optAsset.get();
        asset.internal_status = InternalStatus.COMPLETED;
        jdbi.onDemand(AssetRepository.class).updateAssetNoEvent(asset);
        return true;
    }

    public boolean completeUpload(AssetSmbRequest assetSmbRequest, User user) {
        if(assetSmbRequest.asset() == null) {
            throw new IllegalArgumentException("Asset cannot be null");
        }
        if(assetSmbRequest.shareName() == null) {
            throw new IllegalArgumentException("Share id cannot be null");
        }
        Optional<Asset> optAsset = getAsset(assetSmbRequest.asset().guid());
        if(optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }
        //Mark as asset received
        Asset asset = optAsset.get();
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
        if(assetStatus != InternalStatus.ERDA_ERROR && assetStatus != InternalStatus.SMB_ERROR) {
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
        Optional<Asset> assetOpt = getAsset(updatedAsset.guid);
        if(assetOpt.isEmpty()) {
            throw new IllegalArgumentException("Asset " + updatedAsset.guid + " does not exist");
        }
        if(Strings.isNullOrEmpty(updatedAsset.updateUser)) {
            throw new IllegalArgumentException("Update user must be provided");
        }
        validateAsset(updatedAsset );
        Asset existing = assetOpt.get();
        existing.tags = updatedAsset.tags;
        existing.workstation= updatedAsset.workstation;
        existing.pipeline = updatedAsset.pipeline;
        existing.pushed_to_specify_date = updatedAsset.pushed_to_specify_date;
        existing.status = updatedAsset.status;
        existing.asset_locked = updatedAsset.asset_locked;
        existing.subject = updatedAsset.subject;
        existing.restricted_access = updatedAsset.restricted_access;
        existing.funding = updatedAsset.funding;
        existing.file_formats = updatedAsset.file_formats;
        existing.payload_type = updatedAsset.payload_type;
        existing.digitizer = updatedAsset.digitizer;
        existing.parent_guid = updatedAsset.parent_guid;
        existing.updateUser = updatedAsset.updateUser;
        validateAssetFields(existing);
        jdbi.onDemand(AssetRepository.class).updateAsset(existing);
        return existing;
    }

    void validateAssetFields(Asset a) {
        if(a.pid == null){
            throw new IllegalArgumentException("PID cannot be null");
        }
        if(a.guid == null) {
            throw new IllegalArgumentException("GUID cannot be null");
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
        if(asset.guid.equals(asset.parent_guid)) {
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



    public Asset persistAsset(Asset asset, User user) {
        Optional<Asset> assetOpt = getAsset(asset.guid);
        if(assetOpt.isPresent()) {
            throw new IllegalArgumentException("Asset " + asset.guid + " already exists");
        }
        validateAssetFields(asset);
        validateAsset(asset);

        // Default values on creation
        asset.last_updated_date = Instant.now();
        asset.created_date = Instant.now();
        asset.internal_status = InternalStatus.METADATA_RECEIVED;
        jdbi.onDemand(AssetRepository.class).createAsset(asset);
        asset.sambaInfo = fileProxyClient.openSamba(new MinimalAsset(asset.guid, asset.parent_guid), user);
        this.statisticsDataService.addAssetToCache(asset);
        return asset;
    }

    public Optional<Asset> getAsset(String assetGuid) {
        return jdbi.onDemand(AssetRepository.class).readAsset(assetGuid);
    }

}
