package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.repositories.AssetRepository;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AssetService {
    private final InstitutionService institutionService;
    private final CollectionService collectionService;
    private final AssetRepository assetRepository;

    @Inject
    public AssetService(InstitutionService institutionService, CollectionService collectionService, AssetRepository assetRepository) {
        this.institutionService = institutionService;
        this.collectionService = collectionService;
        this.assetRepository = assetRepository;
    }

    public Asset persistAsset(Asset asset) {
        System.out.println(asset);
        Optional<Institution> ifExists = institutionService.getIfExists(asset.institution);
        if(ifExists.isEmpty()){
            throw new IllegalArgumentException("Institute doesnt exist");
        }
        Optional<Collection> collectionOpt = collectionService.findCollection(asset.collection);
        if(collectionOpt.isEmpty()) {
            throw new IllegalArgumentException("Collection doesnt exist");
        }
        if(!asset.specimen_barcodes.isEmpty()) {
            //TODO ensure created
        }
        assetRepository.persistAsset(asset);
        return asset;

    }

//    public List<Collection> listCollections(Institution institution) {
//        return collectionRepository.listCollections(institution);
//
//    }
}
