package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.repositories.AssetRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
public class AssetService {
    private final InstitutionService institutionService;
    private final CollectionService collectionService;
    private final WorkstationService workstationService;
    private final Jdbi jdbi;

    @Inject
    public AssetService(InstitutionService institutionService, CollectionService collectionService, WorkstationService workstationService, Jdbi jdbi) {
        this.institutionService = institutionService;
        this.collectionService = collectionService;
        this.workstationService = workstationService;
        this.jdbi = jdbi;
    }

    public Asset updateAsset(Asset updatedAsset) {
        Optional<Asset> assetOpt = getAsset(updatedAsset.guid);
        if(assetOpt.isEmpty()) {
            throw new IllegalArgumentException("Asset " + updatedAsset.guid + " does not exist");
        }
        validateAsset(updatedAsset);
        Asset existing = assetOpt.get();
        if(existing.asset_locked) {
            throw new RuntimeException("Asset is locked");
        }
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

        jdbi.onDemand(AssetRepository.class).updateAsset(existing);
        return updatedAsset;
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
            throw new RuntimeException("Workstation [" + workstation.status() + "] is marked as out of service");
        }

    }

    public Asset persistAsset(Asset asset) {
        Optional<Asset> assetOpt = getAsset(asset.guid);
        if(assetOpt.isPresent()) {
            throw new IllegalArgumentException("Asset " + asset.guid + " already exists");
        }
        validateAsset(asset);

        // Default values on creation
        asset.last_updated_date = Instant.now();
        asset.created_date = Instant.now();
        asset.internal_status = InternalStatus.METADATA_RECEIVED;
        jdbi.onDemand(AssetRepository.class).createAsset(asset);
        asset.asset_location = "/" + asset.institution + "/" + asset.collection + "/" + asset.guid;
        return asset;
    }

    public Optional<Asset> getAsset(String assetGuid) {
        return jdbi.onDemand(AssetRepository.class).readAsset(assetGuid);
    }

}
