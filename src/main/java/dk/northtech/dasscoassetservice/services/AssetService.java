package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.InternalStatus;
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
    private final Jdbi jdbi;

    @Inject
    public AssetService(InstitutionService institutionService, CollectionService collectionService, Jdbi jdbi) {
        this.institutionService = institutionService;
        this.collectionService = collectionService;
        this.jdbi = jdbi;
    }

    public Asset persistAsset(Asset asset) {
        Optional<Institution> ifExists = institutionService.getIfExists(asset.institution);
        if(ifExists.isEmpty()){
            throw new IllegalArgumentException("Institute doesnt exist");
        }
        Optional<Collection> collectionOpt = collectionService.findCollection(asset.collection);
        if(collectionOpt.isEmpty()) {
            throw new IllegalArgumentException("Collection doesnt exist");
        }
        // Default values on creation
        asset.last_updated_date = Instant.now();
        asset.created_date = Instant.now();
        asset.internal_status = InternalStatus.METADATA_RECEIVED;
        jdbi.onDemand(AssetRepository.class).createAsset(asset);
        return asset;
    }

    public Asset getAsset(String assetGuid) {
        return jdbi.onDemand(AssetRepository.class).readAsset(assetGuid).orElse(null);
    }

}
