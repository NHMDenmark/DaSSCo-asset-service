package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.repositories.AssetGroupRepository;
import dk.northtech.dasscoassetservice.repositories.BulkUpdateRepository;
import dk.northtech.dasscoassetservice.repositories.UserRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class AssetGroupService {

    private final Jdbi jdbi;

    private final RightsValidationService rightsValidationService;
    private UserService userService;
    private final KeycloakService keycloakService;

    @Inject
    public AssetGroupService(Jdbi jdbi,
                             UserService userService,
                             KeycloakService keycloakService,
                             RightsValidationService rightsValidationService) {
        this.jdbi = jdbi;
        this.rightsValidationService = rightsValidationService;
        this.userService = userService;
        this.keycloakService = keycloakService;
    }

    public Optional<AssetGroup> createAssetGroup(AssetGroup assetGroup, User user) {

        if (assetGroup == null) {
            throw new IllegalArgumentException("Empty body!");
        }

        // Check fields:
        if (assetGroup.group_name == null || assetGroup.group_name.isEmpty()) {
            throw new IllegalArgumentException("Asset group needs a name!");
        }

        if (assetGroup.assets == null) {
            assetGroup.assets = new ArrayList<>();
        }

        // Lowercase the asset group name for case-insensitivity:
        assetGroup.group_name = assetGroup.group_name.toLowerCase();

        List<Asset> assets = assetGroup.assets.isEmpty()
                ? List.of()
                : jdbi.onDemand(BulkUpdateRepository.class).readMultipleAssets(assetGroup.assets);
        if (assets.size() != assetGroup.assets.size()) {
            throw new IllegalArgumentException("One or more assets were not found!");
        }

        // Check if the group name already exists
        Optional<AssetGroup> assetGroupOptional = jdbi.onDemand(AssetGroupRepository.class).readAssetGroup(assetGroup.group_name);
        if (assetGroupOptional.isPresent()) {
            throw new IllegalArgumentException("Asset group already exists!");
        }

        if (assetGroup.hasAccess == null && assetGroup.keycloakUsers == null) {
            throw new IllegalArgumentException("hasAccess cannot be null");
        }

        if (hasUsersToShare(assetGroup)) {
            List<String> assetsWithoutPermission = new ArrayList<>();
            // Check user roles. You need WRITE access to create the asset group and invite people to it.
            for (Asset asset : assets) {
                boolean hasAccess = rightsValidationService.checkRightsAsset(user, asset, true);
                if (!hasAccess) {
                    assetsWithoutPermission.add(asset.asset_guid);
                }
            }
            if (!assetsWithoutPermission.isEmpty()) {
                throw new DasscoIllegalActionException("You need write permission for every asset before sharing this asset group.", assetsWithoutPermission.toString());
            }

            List<String> usernames = resolveAccessUsernames(assetGroup.hasAccess, assetGroup.keycloakUsers);

            // This gives read access to the Assets in the group:
            jdbi.onDemand(AssetGroupRepository.class).createAssetGroup(assetGroup, user);
            Optional<AssetGroup> optAssetGroup = jdbi.onDemand(AssetGroupRepository.class).grantAccessToAssetGroup(usernames, assetGroup.group_name);
            if (optAssetGroup.isEmpty()) {
                throw new IllegalArgumentException("There has been an error creating the asset group");
            }

        } else {
            List<String> assetsWithoutPermission = new ArrayList<>();
            // Check user roles, you need READ to be able to create an asset group:
            for (Asset asset : assets) {
                boolean hasAccess = rightsValidationService.checkReadRights(user, asset.institution, asset.collection);
                if (!hasAccess) {
                    assetsWithoutPermission.add(asset.asset_guid);
                }
            }
            if (!assetsWithoutPermission.isEmpty()) {
                throw new DasscoIllegalActionException("FORBIDDEN, User does not have read access to all assets.", assetsWithoutPermission.toString());
            }
            jdbi.onDemand(AssetGroupRepository.class).createAssetGroup(assetGroup, user);
        }
        return jdbi.onDemand(AssetGroupRepository.class).readAssetGroup(assetGroup.group_name);
    }

    private boolean hasUsersToShare(AssetGroup assetGroup) {
        return (assetGroup.keycloakUsers != null && !assetGroup.keycloakUsers.isEmpty())
                || (assetGroup.hasAccess != null && !assetGroup.hasAccess.isEmpty());
    }

    private List<String> resolveAccessUsernames(List<String> usernames, List<KeycloakUser> keycloakUsers) {
        if (keycloakUsers != null && !keycloakUsers.isEmpty()) {
            return this.userService.persistKeycloakUsers(keycloakUsers).stream().map(u -> u.username).toList();
        }

        if (usernames == null || usernames.isEmpty()) {
            return List.of();
        }

        for (String username : usernames) {
            if (userService.getUserIfExists(username).isEmpty()) {
                throw new IllegalArgumentException("One or more users to share the Asset Group were not found");
            }
        }
        return usernames;
    }

    public List<Asset> readAssetGroup(String groupName, User user) {
        Optional<AssetGroup> assetGroupOptional = jdbi.onDemand(AssetGroupRepository.class).readAssetGroup(groupName.toLowerCase());
        if (assetGroupOptional.isPresent()) {
            AssetGroup assetGroup = assetGroupOptional.get();
            rightsValidationService.checkReadRightsThrowing(user, assetGroup);
            return jdbi.onDemand(BulkUpdateRepository.class).readMultipleAssets(assetGroup.assets);
        } else {
            throw new IllegalArgumentException("Asset group does not exist!");
        }
    }

    public List<Asset> readAssetGroup(Integer groupId, User user) {
        AssetGroup assetGroup = readAssetGroupById(groupId);
        rightsValidationService.checkReadRightsThrowing(user, assetGroup);
        return jdbi.onDemand(BulkUpdateRepository.class).readMultipleAssets(assetGroup.assets);
    }

    private AssetGroup readAssetGroupById(Integer groupId) {
        if (groupId == null) {
            throw new IllegalArgumentException("Asset group id is required!");
        }

        return jdbi.onDemand(AssetGroupRepository.class)
                .readAssetGroupById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Asset group does not exist!"));
    }

    public List<AssetGroup> readListAssetGroup(User user) {
        return jdbi.onDemand(AssetGroupRepository.class).readListAssetGroup(user);
    }

    public List<AssetGroup> readOwnedAssetGroups(User user) {
        if (user.roles.contains(SecurityRoles.ADMIN)) {
            return jdbi.onDemand(AssetGroupRepository.class).readListAssetGroup(user);
        } else {
            return jdbi.onDemand(AssetGroupRepository.class).readOwnedListAssetGroup(user);
        }
    }

    public void deleteAssetGroup(String groupName, User user) {


        // Only the creator of the asset group can delete it:
        Optional<AssetGroup> assetGroupOptional = jdbi.onDemand(AssetGroupRepository.class).readAssetGroup(groupName.toLowerCase());
        if (assetGroupOptional.isEmpty()) {
            throw new IllegalArgumentException("Asset group does not exist!");
        }

        rightsValidationService.checkAssetGroupOwnershipThrowing(user, assetGroupOptional.get());

        jdbi.onDemand(AssetGroupRepository.class).deleteAssetGroup(groupName.toLowerCase());
    }

    public void deleteAssetGroup(Integer groupId, User user) {

        List<AssetGroup> foundGroups = jdbi.onDemand(AssetGroupRepository.class).readAssetGroupFromGroupIds(List.of(groupId));
        if (foundGroups.isEmpty()) {
            throw new IllegalArgumentException("Asset group does not exist!");
        }

        AssetGroup foundGroup = foundGroups.get(0);
        if (!isCreatorOrAdmin(foundGroup, user)) {
            throw new IllegalArgumentException("Cannot delete asset group. User is not the creator of this asset group: " +
                    "id " + groupId + ", name " + foundGroup.group_name + ".");
        }

        jdbi.onDemand(AssetGroupRepository.class).deleteAssetGroups(List.of(groupId), user);
    }

    public boolean deleteAssetGroups(List<Integer> groupIds, User user) {

        if (groupIds == null || groupIds.isEmpty()) {
            throw new IllegalArgumentException("No asset group ids were provided.");
        }

        // De-dupe ids to avoid redundant processing and clearer validation
        List<Integer> uniqueGroupIds = groupIds.stream().distinct().toList();

        List<AssetGroup> assetGroups = jdbi.onDemand(AssetGroupRepository.class).readAssetGroupFromGroupIds(uniqueGroupIds);
        if (assetGroups.isEmpty()) {
            throw new IllegalArgumentException("Asset groups do not exist!");
        }

        java.util.Map<Integer, AssetGroup> groupsById = assetGroups.stream()
                .collect(java.util.stream.Collectors.toMap(
                        g -> g.group_id,
                        g -> g,
                        (a, b) -> a
                ));

        List<Integer> missingIds = uniqueGroupIds.stream()
                .filter(id -> !groupsById.containsKey(id))
                .toList();
        if (!missingIds.isEmpty()) {
            throw new IllegalArgumentException("Cannot delete asset groups. The following asset group ids were not found: " + missingIds);
        }

        List<Integer> unauthorized = uniqueGroupIds.stream()
                .filter(id -> !isCreatorOrAdmin(groupsById.get(id), user))
                .toList();
        if (!unauthorized.isEmpty()) {
            String unauthorizedNames = unauthorized.stream()
                    .map(id -> {
                        AssetGroup group = groupsById.get(id);
                        return group.group_name + " (id=" + id + ")";
                    })
                    .toList()
                    .toString();
            throw new IllegalArgumentException("Cannot delete asset groups. User is not the creator of: " + unauthorizedNames + ".");
        }

        return jdbi.onDemand(AssetGroupRepository.class).deleteAssetGroups(uniqueGroupIds, user);
    }

    private boolean isCreatorOrAdmin(AssetGroup assetGroup, User user) {
        return rightsValidationService.checkAdminRoles(user) || user.username.equals(assetGroup.groupCreator);
    }


    public AssetGroup addAssetsToAssetGroup(String groupName, List<String> assetList, User user) {

        if (assetList == null) {
            throw new IllegalArgumentException("Empty body!");
        }

        Optional<AssetGroup> assetGroupOptional = jdbi.onDemand(AssetGroupRepository.class).readAssetGroup(groupName.toLowerCase());
        if (assetGroupOptional.isEmpty()) {
            throw new IllegalArgumentException("Asset group does not exist!");
        }

        if (assetList.isEmpty()) {
            throw new IllegalArgumentException("Asset Group has to have assets!");
        }

        List<Asset> assets = jdbi.onDemand(BulkUpdateRepository.class).readMultipleAssets(assetList);
        if (assets.size() != assetList.size()) {
            throw new IllegalArgumentException("One or more assets were not found!");
        }

        // Check if User has access to the asset Group:
        rightsValidationService.checkAssetGroupOwnershipThrowing(user, assetGroupOptional.get());

        // Check if user has access to the assets they want to add (remember, if shared asset group they need write role):
        if (assetGroupOptional.get().hasAccess.size() > 1) {
            List<String> forbiddenAssets = new ArrayList<>();
            for (Asset asset : assets) {
                boolean hasAccess = rightsValidationService.checkRightsAsset(user, asset, true);
                if (!hasAccess) {
                    forbiddenAssets.add(asset.asset_guid);
                }
            }
            if (!forbiddenAssets.isEmpty()) {
                throw new DasscoIllegalActionException("FORBIDDEN. User does not have read access to all assets.", forbiddenAssets.toString());
            }
        } else {
            List<String> forbiddenAssets = new ArrayList<>();
            for (Asset asset : assets) {
                boolean hasAccess = rightsValidationService.checkReadRights(user, asset.institution, asset.collection);
                if (!hasAccess) {
                    forbiddenAssets.add(asset.asset_guid);
                }
            }
            if (!forbiddenAssets.isEmpty()) {
                throw new DasscoIllegalActionException("FORBIDDEN. User does not have read access to all assets.", forbiddenAssets.toString());
            }
        }

        // Everything ok! Proceed to the adding of the assets from the Asset Group:
        Optional<AssetGroup> updateAssetGroup = jdbi.onDemand(AssetGroupRepository.class).addAssetsToAssetGroup(assetList, groupName);
        if (updateAssetGroup.isPresent()) {
            return updateAssetGroup.get();
        } else {
            throw new IllegalArgumentException("Something went wrong.");
        }
    }

    public AssetGroup addAssetsToAssetGroup(Integer groupId, List<String> assetList, User user) {
        AssetGroup assetGroup = readAssetGroupById(groupId);
        return addAssetsToAssetGroup(assetGroup.group_name, assetList, user);
    }

    public AssetGroup removeAssetsFromAssetGroup(String groupName, List<String> assetList, User user) {
        if (assetList == null) {
            throw new IllegalArgumentException("Empty body!");
        }

        Optional<AssetGroup> assetGroupOptional = jdbi.onDemand(AssetGroupRepository.class).readAssetGroup(groupName.toLowerCase());
        if (assetGroupOptional.isEmpty()) {
            throw new IllegalArgumentException("Asset group does not exist!");
        }

        if (assetList.isEmpty()) {
            throw new IllegalArgumentException("Asset Group has to have assets!");
        }

        Set<String> assetSet = new HashSet<>(assetList); // Keep unique.
        assetList = new ArrayList<>(assetSet);

        List<Asset> assets = jdbi.onDemand(BulkUpdateRepository.class).readMultipleAssets(assetList);
        if (assets.size() != assetList.size()) {
            throw new IllegalArgumentException("One or more assets were not found!");
        }

        // Check if User has access to the asset Group:
        rightsValidationService.checkAssetGroupOwnershipThrowing(user, assetGroupOptional.get());

        // Check if list of assets from asset group and list of assets from frontend are equal: Delete Asset Group if they are:
        Set<String> assetGroupAssets = new HashSet<>(assetGroupOptional.get().assets);
        if (assetGroupAssets.equals(assetSet)) {
            jdbi.onDemand(AssetGroupRepository.class).deleteAssetGroup(groupName);
            return new AssetGroup();
        }

        // Everything ok! Proceed to the deletion of the assets from the Asset Group:
        Optional<AssetGroup> updateAssetGroup = jdbi.onDemand(AssetGroupRepository.class).removeAssetsFromAssetGroup(assetList, groupName);
        if (updateAssetGroup.isPresent()) {
            return updateAssetGroup.get();
        } else {
            throw new IllegalArgumentException("Something went wrong.");
        }
    }

    public AssetGroup removeAssetsFromAssetGroup(Integer groupId, List<String> assetList, User user) {
        AssetGroup assetGroup = readAssetGroupById(groupId);
        return removeAssetsFromAssetGroup(assetGroup.group_name, assetList, user);
    }

    public AssetGroup grantKeycloakUserAccessToAssetGroup(String groupName, List<KeycloakUser> keycloakUsers, User user) {
        Optional<AssetGroup> assetGroupOptional = jdbi.onDemand(AssetGroupRepository.class).readAssetGroup(groupName.toLowerCase());
        if (assetGroupOptional.isEmpty()) {
            throw new IllegalArgumentException("Asset group does not exist!");
        }

        if (keycloakUsers == null || keycloakUsers.isEmpty()) {
            throw new IllegalArgumentException("There needs to be a list of keycloak user IDs");
        }








        AssetGroup found = assetGroupOptional.get();
        List<Asset> assets = jdbi.onDemand(BulkUpdateRepository.class).readMultipleAssets(found.assets);
        if (!assets.isEmpty()) {
            List<String> forbiddenAssets = new ArrayList<>();
            for (Asset asset : assets) {
                boolean hasAccess = rightsValidationService.checkRightsAsset(user, asset, true);
                if (!hasAccess) {
                    forbiddenAssets.add(asset.asset_guid);
                }
            }
            if (!forbiddenAssets.isEmpty()) {
                throw new DasscoIllegalActionException("You need write permission for every asset in this group before granting access.", forbiddenAssets.toString());
            }
        }


        rightsValidationService.checkAssetGroupOwnershipThrowing(user, found);

        List<String> usernames = resolveAccessUsernames(null, keycloakUsers);
        Optional<AssetGroup> optAssetGroup = jdbi.onDemand(AssetGroupRepository.class).grantAccessToAssetGroup(usernames, groupName);
        if (optAssetGroup.isEmpty()) {
            throw new IllegalArgumentException("There has been an error updating the asset group");
        }

        return optAssetGroup.get();
    }

    public AssetGroup grantKeycloakUserAccessToAssetGroup(Integer groupId, List<KeycloakUser> keycloakUsers, User user) {
        AssetGroup assetGroup = readAssetGroupById(groupId);
        return grantKeycloakUserAccessToAssetGroup(assetGroup.group_name, keycloakUsers, user);
    }

    public AssetGroup grantAccessToAssetGroup(String groupName, List<String> users, User user) {
        if (users == null || users.isEmpty()) {
            throw new IllegalArgumentException("There needs to be a list of Users");
        }

        List<String> usernames = resolveAccessUsernames(users, null);

        Optional<AssetGroup> assetGroupOptional = jdbi.onDemand(AssetGroupRepository.class).readAssetGroup(groupName.toLowerCase());
        if (assetGroupOptional.isEmpty()) {
            throw new IllegalArgumentException("Asset group does not exist!");
        }

        AssetGroup found = assetGroupOptional.get();
        List<Asset> assets = jdbi.onDemand(BulkUpdateRepository.class).readMultipleAssets(found.assets);
        if (!assets.isEmpty()) {
            List<String> forbiddenAssets = new ArrayList<>();
            for (Asset asset : assets) {
                boolean hasAccess = rightsValidationService.checkRightsAsset(user, asset, true);
                if (!hasAccess) {
                    forbiddenAssets.add(asset.asset_guid);
                }
            }
            if (!forbiddenAssets.isEmpty()) {
                throw new DasscoIllegalActionException("You need write permission for every asset in this group before granting access.", forbiddenAssets.toString());
            }
        }

        rightsValidationService.checkAssetGroupOwnershipThrowing(user, found);

        Optional<AssetGroup> optAssetGroup = jdbi.onDemand(AssetGroupRepository.class).grantAccessToAssetGroup(usernames, groupName);
        if (optAssetGroup.isEmpty()) {
            throw new IllegalArgumentException("There has been an error updating the asset group");
        }

        return optAssetGroup.get();
    }

    public AssetGroup grantAccessToAssetGroup(Integer groupId, List<String> users, User user) {
        AssetGroup assetGroup = readAssetGroupById(groupId);
        return grantAccessToAssetGroup(assetGroup.group_name, users, user);
    }

    public AssetGroup revokeAccessToAssetGroup(String groupName, List<String> users, User user) {

        Optional<AssetGroup> assetGroupOptional = jdbi.onDemand(AssetGroupRepository.class).readAssetGroup(groupName.toLowerCase());
        if (assetGroupOptional.isEmpty()) {
            throw new IllegalArgumentException("Asset group does not exist!");
        }
        rightsValidationService.checkAssetGroupOwnershipThrowing(user, assetGroupOptional.get());

        if (users == null || users.isEmpty()) {
            throw new IllegalArgumentException("There needs to be a list of Users");
        }

        // Check if all the users exist!
        for (String username : users) {
            if (userService.getUserIfExists(username).isEmpty()) {
                throw new IllegalArgumentException("One or more users to share the Asset Group were not found");
            }
        }

        Optional<AssetGroup> optAssetGroup = jdbi.onDemand(AssetGroupRepository.class).revokeAccessToAssetGroup(users, groupName);
        if (optAssetGroup.isEmpty()) {
            throw new IllegalArgumentException("There has been an error updating the asset");
        }

        return optAssetGroup.get();
    }

    public AssetGroup revokeAccessToAssetGroup(Integer groupId, List<String> users, User user) {
        AssetGroup assetGroup = readAssetGroupById(groupId);
        return revokeAccessToAssetGroup(assetGroup.group_name, users, user);
    }
}
