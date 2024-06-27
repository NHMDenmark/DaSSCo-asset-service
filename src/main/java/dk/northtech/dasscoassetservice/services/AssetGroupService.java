package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.AssetGroup;
import dk.northtech.dasscoassetservice.repositories.AssetGroupRepository;
import dk.northtech.dasscoassetservice.repositories.AssetRepository;
import dk.northtech.dasscoassetservice.webapi.v1.AssetGroups;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AssetGroupService {

    private final Jdbi jdbi;

    @Inject
    public AssetGroupService(Jdbi jdbi){
        this.jdbi = jdbi;
    }

    public void createAssetGroup(AssetGroup assetGroup){

        // Check fields:
        if (assetGroup.group_name == null || assetGroup.group_name.isEmpty()){
            throw new IllegalArgumentException("Asset group needs a name!");
        }

        if (assetGroup.assets == null || assetGroup.assets.isEmpty()){
            throw new IllegalArgumentException("Asset group needs assets!");
        }

        // Lowercase the asset group name for case-insensitivity:
        assetGroup.group_name = assetGroup.group_name.toLowerCase();

        List<Asset> assets = jdbi.onDemand(AssetRepository.class).readMultipleAssets(assetGroup.assets);
        if (assets.size() != assetGroup.assets.size()){
            throw new IllegalArgumentException("One or more assets were not found!");
        }

        // Check if the group name already exists
        Optional<AssetGroup> assetGroupOptional = jdbi.onDemand(AssetGroupRepository.class).readAssetGroup(assetGroup.group_name);
        if (assetGroupOptional.isPresent()){
            throw new IllegalArgumentException("Asset group already exists!");
        }

        // Then:
        jdbi.onDemand(AssetGroupRepository.class).createAssetGroup(assetGroup);
    }

    public List<Asset> readAssetGroup(String groupName){

        Optional<AssetGroup> assetGroupOptional = jdbi.onDemand(AssetGroupRepository.class).readAssetGroup(groupName.toLowerCase());
        if (assetGroupOptional.isPresent()){
            AssetGroup assetGroup = assetGroupOptional.get();
            return jdbi.onDemand(AssetRepository.class).readMultipleAssets(assetGroup.assets);
        } else {
            throw new IllegalArgumentException("Asset group does not exist!");
        }
    }

    public List<AssetGroup> readListAssetGroup(){
        return jdbi.onDemand(AssetGroupRepository.class).readListAssetGroup();
    }

    public void deleteAssetGroup(String groupName){

        Optional<AssetGroup> assetGroupOptional = jdbi.onDemand(AssetGroupRepository.class).readAssetGroup(groupName.toLowerCase());
        if (assetGroupOptional.isEmpty()){
            throw new IllegalArgumentException("Asset group does not exist!");
        }

        jdbi.onDemand(AssetGroupRepository.class).deleteAssetGroup(groupName.toLowerCase());
    }

    public List<Asset> updateAssetGroup(String groupName, List<String> assetList){
        Optional<AssetGroup> assetGroupOptional = jdbi.onDemand(AssetGroupRepository.class).readAssetGroup(groupName.toLowerCase());
        if (assetGroupOptional.isEmpty()) {
            throw new IllegalArgumentException("Asset group does not exist!");
        }

        List<Asset> assets = jdbi.onDemand(AssetRepository.class).readMultipleAssets(assetList);
        if (assets.size() != assetList.size()){
            throw new IllegalArgumentException("One or more assets were not found!");
        }

        return jdbi.onDemand(AssetGroupRepository.class).updateAssetGroup(groupName.toLowerCase(),assetList);
    }
}
