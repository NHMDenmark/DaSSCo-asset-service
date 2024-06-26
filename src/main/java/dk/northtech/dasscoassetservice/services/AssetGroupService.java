package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.AssetGroup;
import dk.northtech.dasscoassetservice.repositories.AssetGroupRepository;
import dk.northtech.dasscoassetservice.repositories.AssetRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssetGroupService {

    private final Jdbi jdbi;

    @Inject
    public AssetGroupService(Jdbi jdbi){
        this.jdbi = jdbi;
    }

    public void createAssetGroup(AssetGroup assetGroup){
        // Logic here.
        // Corner cases.
        // Look for assets to see they all exist.
        List<Asset> assets = jdbi.onDemand(AssetRepository.class).readMultipleAssets(assetGroup.assets);
        if (assets.size() != assetGroup.assets.size()){
            throw new IllegalArgumentException("One or more assets were not found!");
        }

        // Check if the group name already exists


        // Then:
        jdbi.onDemand(AssetGroupRepository.class).createAssetGroup(assetGroup);
    }
}
