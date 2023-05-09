package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.repositories.AssetRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

    public Asset persistAsset(Asset asset) {
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
        Optional<Asset> assetOpt = getAsset(asset.guid);
        if(assetOpt.isPresent()) {
            throw new IllegalArgumentException("Asset " + asset.guid + " already exists");
        }

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
