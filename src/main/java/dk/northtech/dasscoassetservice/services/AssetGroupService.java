package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.repositories.AssetGroupRepository;
import dk.northtech.dasscoassetservice.repositories.AssetRepository;
import dk.northtech.dasscoassetservice.repositories.UserRepository;
import dk.northtech.dasscoassetservice.webapi.v1.AssetGroups;
import jakarta.inject.Inject;
import org.checkerframework.checker.nullness.Opt;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

import java.awt.color.ICC_ColorSpace;
import java.util.List;
import java.util.Optional;

@Service
public class AssetGroupService {

    private final Jdbi jdbi;

    private final RightsValidationService rightsValidationService;

    @Inject
    public AssetGroupService(Jdbi jdbi,
                             RightsValidationService rightsValidationService){
        this.jdbi = jdbi;
        this.rightsValidationService = rightsValidationService;
    }

    public void createAssetGroup(AssetGroup assetGroup, User user){

        if(assetGroup == null){
            throw new IllegalArgumentException("Empty body!");
        }

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

        if (assetGroup.hasAccess == null){
            throw new IllegalArgumentException("hasAccess cannot be null");
        }

        if (!assetGroup.hasAccess.isEmpty()){
            // Check user roles. You need WRITE access to create the asset group and invite people to it.
            for (Asset asset: assets){
                rightsValidationService.checkWriteRightsThrowing(user, asset.institution, asset.collection);
            }

            // Check if all the users exist!
            for (String username : assetGroup.hasAccess){
                if(!jdbi.onDemand(UserRepository.class).getUserByUsername(username)){
                    throw new IllegalArgumentException("One or more users to share the Asset Group were not found");
                }
            }
            // This gives read access to the Assets in the group:
            jdbi.onDemand(AssetGroupRepository.class).createSharedAssetGroup(assetGroup, user);

        } else {
            // Check user roles, you need READ to be able to create an asset group:
            for (Asset asset : assets){
                rightsValidationService.checkReadRightsThrowing(user, asset.institution, asset.collection);
            }

            // Then:
            jdbi.onDemand(AssetGroupRepository.class).createAssetGroup(assetGroup, user);
        }
    }

    public List<Asset> readAssetGroup(String groupName, User user){
        Optional<AssetGroup> assetGroupOptional = jdbi.onDemand(AssetGroupRepository.class).readAssetGroup(groupName.toLowerCase());
        if (assetGroupOptional.isPresent()){
            AssetGroup assetGroup = assetGroupOptional.get();
            rightsValidationService.checkReadRightsThrowing(user, assetGroup);
            return jdbi.onDemand(AssetRepository.class).readMultipleAssets(assetGroup.assets);
        } else {
            throw new IllegalArgumentException("Asset group does not exist!");
        }
    }

    public List<AssetGroup> readListAssetGroup(User user){
        if (user.roles.contains(SecurityRoles.ADMIN) || user.roles.contains(SecurityRoles.DEVELOPER) || user.roles.contains(SecurityRoles.SERVICE)){
            return jdbi.onDemand(AssetGroupRepository.class).readListAssetGroup(false, user);
        }
        else {
            return jdbi.onDemand(AssetGroupRepository.class).readListAssetGroup(true, user);
        }
    }

    public void deleteAssetGroup(String groupName, User user){

        // Only the creator of the asset group can delete it:
        Optional<AssetGroup> assetGroupOptional = jdbi.onDemand(AssetGroupRepository.class).readAssetGroup(groupName.toLowerCase());
        if (assetGroupOptional.isEmpty()){
            throw new IllegalArgumentException("Asset group does not exist!");
        }

        jdbi.onDemand(AssetGroupRepository.class).deleteAssetGroup(groupName.toLowerCase(), user);
    }

    public AssetGroup addAssetsToAssetGroup(String groupName, List<String> assetList, User user){

        if (assetList == null){
            throw new IllegalArgumentException("Empty body!");
        }

        Optional<AssetGroup> assetGroupOptional = jdbi.onDemand(AssetGroupRepository.class).readAssetGroup(groupName.toLowerCase());
        if (assetGroupOptional.isEmpty()) {
            throw new IllegalArgumentException("Asset group does not exist!");
        }

        if(assetList.isEmpty()){
            throw new IllegalArgumentException("Asset Group has to have assets!");
        }

        List<Asset> assets = jdbi.onDemand(AssetRepository.class).readMultipleAssets(assetList);
        if (assets.size() != assetList.size()){
            throw new IllegalArgumentException("One or more assets were not found!");
        }

        // Check if User has access to the asset Group:
        rightsValidationService.checkAssetGroupOwnershipThrowing(user, assetGroupOptional.get());

        // Check if user has access to the assets they want to add:
        for (Asset asset : assets){
            rightsValidationService.checkReadRightsThrowing(user, asset.institution, asset.collection);
        }

        // Everything ok! Proceed to the adding of the assets from the Asset Group:
        Optional<AssetGroup> updateAssetGroup = jdbi.onDemand(AssetGroupRepository.class).addAssetsToAssetGroup(assetList, groupName);
        if (updateAssetGroup.isPresent()){
            return updateAssetGroup.get();
        } else {
            throw new IllegalArgumentException("Something went wrong.");
        }
    }

    public AssetGroup removeAssetsFromAssetGroup(String groupName, List<String> assetList, User user){
        if (assetList == null){
            throw new IllegalArgumentException("Empty body!");
        }

        Optional<AssetGroup> assetGroupOptional = jdbi.onDemand(AssetGroupRepository.class).readAssetGroup(groupName.toLowerCase());
        if (assetGroupOptional.isEmpty()) {
            throw new IllegalArgumentException("Asset group does not exist!");
        }

        if(assetList.isEmpty()){
            throw new IllegalArgumentException("Asset Group has to have assets!");
        }

        List<Asset> assets = jdbi.onDemand(AssetRepository.class).readMultipleAssets(assetList);
        if (assets.size() != assetList.size()){
            throw new IllegalArgumentException("One or more assets were not found!");
        }

        // Check if User has access to the asset Group:
        rightsValidationService.checkAssetGroupOwnershipThrowing(user, assetGroupOptional.get());

        // Check if user has access to the assets they want to remove:
        for (Asset asset : assets){
            rightsValidationService.checkReadRightsThrowing(user, asset.institution, asset.collection);
        }

        // Everything ok! Proceed to the deletion of the assets from the Asset Group:
        Optional<AssetGroup> updateAssetGroup = jdbi.onDemand(AssetGroupRepository.class).removeAssetsFromAssetGroup(assetList, groupName);
        if (updateAssetGroup.isPresent()){
            return updateAssetGroup.get();
        } else {
            throw new IllegalArgumentException("Something went wrong.");
        }
    }

    public AssetGroup grantAccessToAssetGroup(String groupName, List<String> users, User user){
        if (users == null || users.isEmpty()){
            throw new IllegalArgumentException("There needs to be a list of Users");
        }

        // Check if all the users exist!
        for (String username : users){
            if(!jdbi.onDemand(UserRepository.class).getUserByUsername(username)){
                throw new IllegalArgumentException("One or more users to share the Asset Group were not found");
            }
        }

        Optional<AssetGroup> assetGroupOptional = jdbi.onDemand(AssetGroupRepository.class).readAssetGroup(groupName.toLowerCase());
        if (assetGroupOptional.isEmpty()) {
            throw new IllegalArgumentException("Asset group does not exist!");
        }

        AssetGroup found = assetGroupOptional.get();
        List<Asset> assets = jdbi.onDemand(AssetRepository.class).readMultipleAssets(found.assets);
        if (!assets.isEmpty()){
            for (Asset asset: assets){
                rightsValidationService.checkWriteRightsThrowing(user, asset.institution, asset.collection);
            }
        }

        rightsValidationService.checkAssetGroupOwnershipThrowing(user, found);

        Optional<AssetGroup> optAssetGroup =  jdbi.onDemand(AssetGroupRepository.class).grantAccessToAssetGroup(users, groupName);
        if (optAssetGroup.isEmpty()){
            throw new IllegalArgumentException("There has been an error updating the asset");
        }

        return optAssetGroup.get();
    }

    public AssetGroup revokeAccessToAssetGroup(String groupName, List<String> users, User user){

        if (users == null || users.isEmpty()){
            throw new IllegalArgumentException("There needs to be a list of Users");
        }

        // Check if all the users exist!
        for (String username : users){
            if(!jdbi.onDemand(UserRepository.class).getUserByUsername(username)){
                throw new IllegalArgumentException("One or more users to share the Asset Group were not found");
            }
        }

        Optional<AssetGroup> assetGroupOptional = jdbi.onDemand(AssetGroupRepository.class).readAssetGroup(groupName.toLowerCase());
        if (assetGroupOptional.isEmpty()) {
            throw new IllegalArgumentException("Asset group does not exist!");
        }

        rightsValidationService.checkAssetGroupOwnershipThrowing(user, assetGroupOptional.get());

        Optional<AssetGroup> optAssetGroup =  jdbi.onDemand(AssetGroupRepository.class).revokeAccessToAssetGroup(users, groupName);
        if (optAssetGroup.isEmpty()){
            throw new IllegalArgumentException("There has been an error updating the asset");
        }

        return optAssetGroup.get();
    }
}
