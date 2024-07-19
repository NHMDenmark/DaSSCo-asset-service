package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.A;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AssetGroupServiceTest extends AbstractIntegrationTest{

    @Test
    void testCreateAssetGroupServiceUserNotShared(){
        // Creation of Asset Group as a Service User should work always:
        User user = new User("testCreateAssetGroupServiceUserNotShared");
        user.roles.add("service-user");
        AssetGroup assetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();

        assetGroup.group_name = "test-group";
        assetGroup.assets.add("asset-1");
        assetGroup.assets.add("asset-3");
        assetGroup.assets.add("asset-5");

        // No asset group exist yet:
        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(3);

        assetGroup.hasAccess = new ArrayList<>();

        assetGroupService.createAssetGroup(assetGroup, user);

        // Creation of Asset Group Assertions:
        assetGroupList = assetGroupService.readListAssetGroup(user);

        int index = 0;
        for (int i = 0; i < assetGroupList.size(); i++){
            if (assetGroupList.get(i).group_name.equals("test-group")){
                index = i;
                break;
            }
        }
        assertThat(assetGroupList.get(index).group_name).isEqualTo(assetGroup.group_name);
        assertThat(assetGroupList.get(index).assets.size()).isEqualTo(3);
        assertThat(assetGroupList.get(index).assets.contains("asset-1")).isTrue();
        assertThat(assetGroupList.get(index).assets.contains("asset-3")).isTrue();
        assertThat(assetGroupList.get(index).assets.contains("asset-5")).isTrue();

        // Deletion:
        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
        assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(3);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.readAssetGroup(assetGroup.group_name, user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group does not exist!");
    }

    @Test
    void testCreateAssetGroupServiceUserShared(){
        // Creation of Asset Group as a Service User should work always:
        User user = new User("testCreateAssetGroupServiceUserShared");
        user.roles.add("service-user");
        AssetGroup assetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();

        assetGroup.group_name = "test-group";
        assetGroup.assets.add("asset-1");
        assetGroup.assets.add("asset-3");
        assetGroup.assets.add("asset-5");

        // No asset group exist yet:
        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(3);

        assetGroup.hasAccess = new ArrayList<>();
        assetGroup.hasAccess.add("role-1-user");
        assetGroup.hasAccess.add("role-2-user");

        assetGroupService.createAssetGroup(assetGroup, user);

        // Creation of Asset Group Assertions:
        assetGroupList = assetGroupService.readListAssetGroup(user);

        int index = 0;
        for (int i = 0; i < assetGroupList.size(); i++){
            if (assetGroupList.get(i).group_name.equals("test-group")){
                index = i;
                break;
            }
        }

        assertThat(assetGroupList.get(index).hasAccess.contains("role-1-user")).isTrue();
        assertThat(assetGroupList.get(index).hasAccess.contains("role-2-user")).isTrue();
        assertThat(assetGroupList.get(index).group_name).isEqualTo(assetGroup.group_name);
        assertThat(assetGroupList.get(index).assets.size()).isEqualTo(3);
        assertThat(assetGroupList.get(index).assets.contains("asset-1")).isTrue();
        assertThat(assetGroupList.get(index).assets.contains("asset-3")).isTrue();
        assertThat(assetGroupList.get(index).assets.contains("asset-5")).isTrue();

        // Deletion:
        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
        assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(3);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.readAssetGroup(assetGroup.group_name, user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group does not exist!");
    }

    @Test
    void testAssetGroupsRole1ReadNotShared(){
        // Creation of Asset Group as a READ_role-1 should work:
        User user = new User("testAssetGroupsRole1ReadNotShared");
        user.roles.add("READ_role-1");

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();
        assetGroup.hasAccess = new ArrayList<>();

        assetGroup.group_name = "test-group";
        assetGroup.assets.add("asset-1");
        assetGroup.assets.add("asset-2");
        assetGroup.assets.add("asset-5");

        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);

        assetGroupService.createAssetGroup(assetGroup, user);

        // Creation of Asset Group Assertions:
        assetGroupList = assetGroupService.readListAssetGroup(user);

        int index = 0;
        for (int i = 0; i < assetGroupList.size(); i++){
            if (assetGroupList.get(i).group_name.equals("test-group")){
                index = i;
                break;
            }
        }

        assertThat(assetGroupList.size()).isEqualTo(1);
        assertThat(assetGroupList.get(index).group_name).isEqualTo(assetGroup.group_name);
        assertThat(assetGroupList.get(index).assets.size()).isEqualTo(3);
        assertThat(assetGroupList.get(index).assets.contains("asset-1")).isTrue();
        assertThat(assetGroupList.get(index).assets.contains("asset-2")).isTrue();
        assertThat(assetGroupList.get(index).assets.contains("asset-5")).isTrue();

        // Deletion:
        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
        assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.readAssetGroup(assetGroup.group_name, user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group does not exist!");
    }

    @Test
    void testAssetGroupsRole1WriteShared(){
        User user = new User("testAssetGroupsRole1WriteShared");
        user.roles.add("WRITE_role-1");

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();
        assetGroup.hasAccess = new ArrayList<>();

        assetGroup.group_name = "test-group";
        assetGroup.assets.add("asset-1");
        assetGroup.assets.add("asset-2");
        assetGroup.assets.add("asset-5");

        assetGroup.hasAccess = new ArrayList<>();
        assetGroup.hasAccess.add("role-1-user");
        assetGroup.hasAccess.add("role-2-user");

        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);

        assetGroupService.createAssetGroup(assetGroup, user);

        // Creation of Asset Group Assertions:
        assetGroupList = assetGroupService.readListAssetGroup(user);

        int index = 0;
        for (int i = 0; i < assetGroupList.size(); i++){
            if (assetGroupList.get(i).group_name.equals("test-group")){
                index = i;
                break;
            }
        }

        assertThat(assetGroupList.size()).isEqualTo(1);
        assertThat(assetGroupList.get(index).hasAccess.contains("role-1-user")).isTrue();
        assertThat(assetGroupList.get(index).hasAccess.contains("role-2-user")).isTrue();
        assertThat(assetGroupList.get(index).group_name).isEqualTo(assetGroup.group_name);
        assertThat(assetGroupList.get(index).assets.size()).isEqualTo(3);
        assertThat(assetGroupList.get(index).assets.contains("asset-1")).isTrue();
        assertThat(assetGroupList.get(index).assets.contains("asset-2")).isTrue();
        assertThat(assetGroupList.get(index).assets.contains("asset-5")).isTrue();

        // Deletion:
        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
        assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.readAssetGroup(assetGroup.group_name, user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group does not exist!");
    }

    @Test
    void failTestCreateAssetRole1ReadShared(){
        User user = new User("failTestCreateAssetRole1ReadShared");
        user.roles.add("READ_role-1");

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();
        assetGroup.hasAccess = new ArrayList<>();

        assetGroup.group_name = "test-group";
        assetGroup.assets.add("asset-1");
        assetGroup.assets.add("asset-2");
        assetGroup.assets.add("asset-5");

        assetGroup.hasAccess = new ArrayList<>();
        assetGroup.hasAccess.add("role-1-user");
        assetGroup.hasAccess.add("role-2-user");

        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);

        // Can't add users without WRITE role to the assets:
        assertThrows(DasscoIllegalActionException.class, () -> assetGroupService.createAssetGroup(assetGroup, user));

        assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);
    }

    @Test
    void failTestCreateAssetGroupRole2ReadNotSharedForbidden(){
        User user = new User("failTestCreateAssetGroupRole2ReadNotSharedForbidden");
        user.roles.add("READ_role-2");

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();
        assetGroup.hasAccess = new ArrayList<>();

        assetGroup.group_name = "test-group";
        assetGroup.assets.add("asset-1");
        assetGroup.assets.add("asset-2");
        assetGroup.assets.add("asset-5");

        assetGroup.hasAccess = new ArrayList<>();

        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);

        // Can't add users without WRITE role to the assets:
        assertThrows(DasscoIllegalActionException.class, () -> assetGroupService.createAssetGroup(assetGroup, user));

        assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);
    }

    @Test
    void testCreateAssetGroupNoBody(){
        AssetGroup assetGroup = null;
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.createAssetGroup(assetGroup, user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Empty body!");
    }

    @Test
    void testCreateAssetGroupNoName(){
        User user = new User("testCreateAssetGroupNoName");
        user.roles.add("fail");
        AssetGroup assetGroup = new AssetGroup();
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.createAssetGroup(assetGroup, user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group needs a name!");
        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);
    }

    @Test
    void testCreateAssetGroupNoAssets(){
        User user = new User("testCreateAssetGroupNoAssets");
        user.roles.add("fail");
        AssetGroup assetGroup = new AssetGroup();
        assetGroup.group_name = "asset-group-no-assets";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.createAssetGroup(assetGroup, user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group needs assets!");
        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);
    }

    @Test
    void testCreateAssetGroupNonExistentAssets() {
        User user = new User("testCreateAssetGroupNonExistentAssets");
        user.roles.add("service-user");

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();
        assetGroup.hasAccess = new ArrayList<>();

        assetGroup.group_name = "test-group";
        assetGroup.assets.add("asset-1");
        assetGroup.assets.add("non-existent-asset-2");

        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(3);

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.createAssetGroup(assetGroup, user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("One or more assets were not found!");
        assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(3);
    }

    @Test
    void testCreateAssetGroupAlreadyExists(){

        User user = new User("testCreateAssetGroupAlreadyExists");

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();
        assetGroup.hasAccess = new ArrayList<>();

        assetGroup.group_name = "ag-1";
        assetGroup.assets = new ArrayList<>();
        assetGroup.assets.add("asset-1");

        user.roles.add("service-user");

        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(3);

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.createAssetGroup(assetGroup, user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group already exists!");

        assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(3);
    }


    @Test
    void testReadAssetGroupServiceUser(){
        User user = new User("testReadAssetGroupServiceUser");
        user.roles.add("service-user");

        List<Asset> found = assetGroupService.readAssetGroup("ag-1", user);
        assertThat(found.size()).isEqualTo(3);
    }

    @Test
    void testReadAssetGroupRole1(){
        User user = new User("testReadAssetGroupRole1");
        user.roles.add("READ_role-1");
        AssetGroup assetGroup = new AssetGroup();
        assetGroup.group_name = "test-1";
        assetGroup.assets = new ArrayList<>();
        assetGroup.assets.add("asset-1");
        assetGroup.hasAccess = new ArrayList<>();

        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);

        assetGroupService.createAssetGroup(assetGroup, user);

        assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(1);
        List<Asset> found = assetGroupService.readAssetGroup("test-1", user);
        assertThat(found.size()).isEqualTo(1);

        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
        assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);
    }


    @Test
    void testReadAssetGroupForbidden(){
        User user = new User("testReadAssetGroupForbidden");
        user.roles.add("READ_role-2");
        assertThrows(DasscoIllegalActionException.class, () -> assetGroupService.readAssetGroup("ag-1", user));
    }

    @Test
    void testReadAssetGroupGroupDoesNotExist(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.readAssetGroup("this-asset-does-not-exist", user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group does not exist!");
    }


    @Test
    void testReadListAssetGroupServiceUser(){
        User user = new User("testReadListAssetGroupServiceUser");
        user.roles.add("service-user");

        List<AssetGroup> found = assetGroupService.readListAssetGroup(user);
        assertThat(found.size()).isEqualTo(3);
    }

    @Test
    void testReadListAssetGroupRole1(){
        User newUser = new User("role-1-user");
        List<AssetGroup> found = assetGroupService.readListAssetGroup(newUser);
        assertThat(found.size()).isEqualTo(1);
        user.roles.clear();
    }

    // Delete Asset Group is used extensively in these unit tests. Won't test it explicitly.

    @Test
    void failTestDeleteAssetGroupForbidden(){
        User user = new User("failTestDeleteAssetGroupForbidden");
        user.roles.add("service-user");
        List<AssetGroup> found = assetGroupService.readListAssetGroup(user);
        assertThat(found.size()).isEqualTo(3);

        // Deleting does nothing:
        assetGroupService.deleteAssetGroup("ag-1", user);

        found = assetGroupService.readListAssetGroup(user);
        assertThat(found.size()).isEqualTo(3);
    }

    @Test
    void failTestDeleteAssetGroupGroupDoesNotExist(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.deleteAssetGroup("failing", user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group does not exist!");
    }

    @Test
    void testAddAssetsToAssetGroupReadRole(){
        User user = new User("testAddAssetsToAssetGroupReadRole");
        user.roles.add("READ_role-1");
        AssetGroup assetGroup = new AssetGroup();
        assetGroup.group_name = "new-group";
        assetGroup.assets = new ArrayList<>();
        assetGroup.assets.add("asset-1");
        assetGroup.hasAccess = new ArrayList<>();

        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);

        assetGroupService.createAssetGroup(assetGroup, user);

        assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(1);

        List<Asset> assets = assetGroupService.readAssetGroup(assetGroup.group_name, user);
        assertThat(assets.size()).isEqualTo(1);

        List<String> assetsToAdd = new ArrayList<>();
        assetsToAdd.add("asset-2");

        assetGroupService.addAssetsToAssetGroup(assetGroup.group_name, assetsToAdd, user);
        assets = assetGroupService.readAssetGroup(assetGroup.group_name, user);
        assertThat(assets.size()).isEqualTo(2);

        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);

        assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);
    }

    @Test
    void testAddAssetsToAssetGroupWriteRole(){
        User user = new User("testAddAssetsToAssetGroupWriteRole");
        user.roles.add("WRITE_role-1");
        AssetGroup assetGroup = new AssetGroup();
        assetGroup.group_name = "new-group";
        assetGroup.assets = new ArrayList<>();
        assetGroup.assets.add("asset-1");
        assetGroup.hasAccess = new ArrayList<>();
        assetGroup.hasAccess.add("service-user");

        assetGroupService.createAssetGroup(assetGroup, user);

        List<Asset> assets = assetGroupService.readAssetGroup(assetGroup.group_name, user);
        assertThat(assets.size()).isEqualTo(1);

        List<String> assetsToAdd = new ArrayList<>();
        assetsToAdd.add("asset-2");

        assetGroupService.addAssetsToAssetGroup(assetGroup.group_name, assetsToAdd, user);
        assets = assetGroupService.readAssetGroup(assetGroup.group_name, user);
        assertThat(assets.size()).isEqualTo(2);

        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
    }

    @Test
    void testFailTestAddAssetsToAssetGroupForbidden(){
        User user = new User("testFailTestAddAssetsToAssetGroupForbidden");
        user.roles.add("READ_role-1");
        AssetGroup assetGroup = new AssetGroup();
        assetGroup.group_name = "new-group";
        assetGroup.assets = new ArrayList<>();
        assetGroup.assets.add("asset-1");
        assetGroup.hasAccess = new ArrayList<>();

        assetGroupService.createAssetGroup(assetGroup, user);

        List<String> assets = new ArrayList<>();
        assets.add("asset-2");
        user.roles.clear();
        user.roles.add("READ_role-2");
        assertThrows(DasscoIllegalActionException.class, () -> assetGroupService.addAssetsToAssetGroup(assetGroup.group_name, assets, user));

        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
    }

    @Test
    void testFailAddAssetsToAssetGroupNoBody(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.addAssetsToAssetGroup("someGroup", null, user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Empty body!");
    }

    @Test
    void testFailAddAssetsToAssetGroupAssetDoesNotExist(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.addAssetsToAssetGroup("someGroup", new ArrayList<>(), user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group does not exist!");
    }

    @Test
    void testFailAddAssetsToAssetGroupAssetsIsEmpty(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.addAssetsToAssetGroup("ag-1", new ArrayList<>(), user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset Group has to have assets!");
    }

    @Test
    void testFailAddAssetsToAssetGroupAssetsNotFound(){
        List<String> assets = new ArrayList<>();
        assets.add("asset-1");
        assets.add("non-existent");
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.addAssetsToAssetGroup("ag-1", assets, user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("One or more assets were not found!");
    }

    @Test
    void testRemoveAssetsFromAssetGroupReadRole(){
        User user = new User("testRemoveAssetsFromAssetGroupReadRole");
        user.roles.add("READ_role-1");
        AssetGroup assetGroup = new AssetGroup();
        assetGroup.group_name = "new-group";
        assetGroup.assets = new ArrayList<>();
        assetGroup.assets.add("asset-1");
        assetGroup.assets.add("asset-2");
        assetGroup.hasAccess = new ArrayList<>();

        assetGroupService.createAssetGroup(assetGroup, user);

        List<Asset> assets = assetGroupService.readAssetGroup(assetGroup.group_name, user);
        assertThat(assets.size()).isEqualTo(2);

        List<String> assetsToRemove = new ArrayList<>();
        assetsToRemove.add("asset-2");

        assetGroupService.removeAssetsFromAssetGroup(assetGroup.group_name, assetsToRemove, user);
        assets = assetGroupService.readAssetGroup(assetGroup.group_name, user);
        assertThat(assets.size()).isEqualTo(1);

        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
    }

    @Test
    void testRemoveAssetsFromAssetGroupWriteRole(){
        User user = new User("testRemoveAssetsFromAssetGroupWriteRole");
        user.roles.add("WRITE_role-1");
        AssetGroup assetGroup = new AssetGroup();
        assetGroup.group_name = "new-group";
        assetGroup.assets = new ArrayList<>();
        assetGroup.assets.add("asset-1");
        assetGroup.assets.add("asset-2");
        assetGroup.hasAccess = new ArrayList<>();
        assetGroup.hasAccess.add("service-user");

        assetGroupService.createAssetGroup(assetGroup, user);

        List<Asset> assets = assetGroupService.readAssetGroup(assetGroup.group_name, user);
        assertThat(assets.size()).isEqualTo(2);

        List<String> assetsToRemove = new ArrayList<>();
        assetsToRemove.add("asset-2");

        assetGroupService.removeAssetsFromAssetGroup(assetGroup.group_name, assetsToRemove, user);
        assets = assetGroupService.readAssetGroup(assetGroup.group_name, user);
        assertThat(assets.size()).isEqualTo(1);

        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
    }

    @Test
    void testRemoveAssetsDeletesGroup(){
        User user = new User("testRemoveAssetsDeletesGroup");
        user.roles.add("READ_role-1");
        AssetGroup assetGroup = new AssetGroup();
        assetGroup.group_name = "new-group";
        assetGroup.assets = new ArrayList<>();
        assetGroup.assets.add("asset-1");
        assetGroup.assets.add("asset-2");
        assetGroup.hasAccess = new ArrayList<>();

        assetGroupService.createAssetGroup(assetGroup, user);

        List<Asset> assets = assetGroupService.readAssetGroup(assetGroup.group_name, user);
        assertThat(assets.size()).isEqualTo(2);

        List<String> assetsToRemove = new ArrayList<>();
        assetsToRemove.add("asset-2");
        assetsToRemove.add("asset-1");

        assetGroupService.removeAssetsFromAssetGroup(assetGroup.group_name, assetsToRemove, user);

        assertThat(assetGroupService.readListAssetGroup(user).size()).isEqualTo(0);
    }

    @Test
    void testFailRemoveAssetsToAssetGroupNoBody(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.removeAssetsFromAssetGroup("someGroup", null, user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Empty body!");
    }

    @Test
    void testFailRemoveAssetsToAssetGroupAssetDoesNotExist(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.removeAssetsFromAssetGroup("someGroup", new ArrayList<>(), user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group does not exist!");
    }

    @Test
    void testFailRemoveAssetsToAssetGroupAssetsIsEmpty(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.removeAssetsFromAssetGroup("ag-1", new ArrayList<>(), user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset Group has to have assets!");
    }

    @Test
    void testFailRemoveAssetsToAssetGroupAssetsNotFound(){
        List<String> assets = new ArrayList<>();
        assets.add("asset-1");
        assets.add("non-existent");
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.removeAssetsFromAssetGroup("ag-1", assets, user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("One or more assets were not found!");
    }

    @Test
    void testGrantAccessServiceUser(){
        User user = new User("testGrantAccessServiceUser");
        user.roles.add("service-user");

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.group_name = "new-group";
        assetGroup.assets = new ArrayList<>();
        assetGroup.assets.add("asset-1");
        assetGroup.hasAccess = new ArrayList<>();

        assetGroupService.createAssetGroup(assetGroup, user);

        List<Asset> assets = assetGroupService.readAssetGroup(assetGroup.group_name, user);
        assertThat(assets.size()).isEqualTo(1);

        User newUser = new User("role-2-user");
        user.roles.add("READ_role-2");
        assertThrows(DasscoIllegalActionException.class, () -> assetGroupService.readAssetGroup(assetGroup.group_name, newUser));

        List<String> userList = new ArrayList<>();
        userList.add("role-2-user");

        assetGroupService.grantAccessToAssetGroup(assetGroup.group_name, userList, user);
        // Now the second user has access =)
        assets = assetGroupService.readAssetGroup(assetGroup.group_name, newUser);
        assertThat(assets.size()).isEqualTo(1);

        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
    }

    @Test
    void testGrantAccessWriteRole1(){
        User user = new User("testGrantAccessWriteRole1");
        user.roles.add("WRITE_role-1");

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.group_name = "new-group";
        assetGroup.assets = new ArrayList<>();
        assetGroup.assets.add("asset-1");
        assetGroup.hasAccess = new ArrayList<>();

        assetGroupService.createAssetGroup(assetGroup, user);

        List<Asset> assets = assetGroupService.readAssetGroup(assetGroup.group_name, user);
        assertThat(assets.size()).isEqualTo(1);

        User newUser = new User("role-2-user");
        user.roles.add("READ_role-2");
        assertThrows(DasscoIllegalActionException.class, () -> assetGroupService.readAssetGroup(assetGroup.group_name, newUser));

        List<String> userList = new ArrayList<>();
        userList.add("role-2-user");

        assetGroupService.grantAccessToAssetGroup(assetGroup.group_name, userList, user);
        // Now the second user has access =)
        assets = assetGroupService.readAssetGroup(assetGroup.group_name, newUser);
        assertThat(assets.size()).isEqualTo(1);

        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
    }

    @Test
    void failTestGrantAccessReadRole1(){
        User user = new User("failTestGrantAccessReadRole1");
        user.roles.add("READ_role-1");

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.group_name = "new-group";
        assetGroup.assets = new ArrayList<>();
        assetGroup.assets.add("asset-1");
        assetGroup.hasAccess = new ArrayList<>();

        assetGroupService.createAssetGroup(assetGroup, user);

        List<Asset> assets = assetGroupService.readAssetGroup(assetGroup.group_name, user);
        assertThat(assets.size()).isEqualTo(1);

        List<String> userList = new ArrayList<>();
        userList.add("role-2-user");

        assertThrows(DasscoIllegalActionException.class, () -> assetGroupService.grantAccessToAssetGroup(assetGroup.group_name, userList, user));

        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
    }

    @Test
    void failTestGrantAccessNotTheOwner(){
        User user = new User("failTestGrantAccessNotTheOwner");
        user.roles.add("WRITE_role-1");

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.group_name = "new-group";
        assetGroup.assets = new ArrayList<>();
        assetGroup.assets.add("asset-1");
        assetGroup.hasAccess = new ArrayList<>();

        assetGroupService.createAssetGroup(assetGroup, user);

        List<Asset> assets = assetGroupService.readAssetGroup(assetGroup.group_name, user);
        assertThat(assets.size()).isEqualTo(1);

        User newUser = new User("role-1-user");
        newUser.roles.add("WRITE_role-1");
        List<String> userList = new ArrayList<>();
        userList.add("role-1-user");

        assertThrows(DasscoIllegalActionException.class, () -> assetGroupService.grantAccessToAssetGroup(assetGroup.group_name, userList, newUser));

        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
    }

    @Test
    void failGrantAccessNoUsers(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.grantAccessToAssetGroup("", null, null));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("There needs to be a list of Users");
    }

    @Test
    void failGrantAccessNoUserFound(){
        List<String> users = new ArrayList<>();
        users.add("non-existent-user");
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.grantAccessToAssetGroup("", users, null));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("One or more users to share the Asset Group were not found");
    }

    @Test
    void failGrantAccessNoAssetGroupFound(){
        List<String> users = new ArrayList<>();
        users.add("role-1-user");
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.grantAccessToAssetGroup("non-existent-group", users, null));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group does not exist!");
    }

    public Asset getTestAsset(String guid) {
        Asset asset = new Asset();
        asset.asset_pid = guid + "-pid";
        asset.status = AssetStatus.BEING_PROCESSED;
        asset.asset_locked = false;
        asset.digitiser = "Karl-Børge";
        asset.asset_guid = guid;
        asset.funding = "Hundredetusindvis af dollars";
        asset.date_asset_taken = Instant.now();
        asset.subject = "Folder";
        asset.file_formats = Arrays.asList(FileFormat.JPEG);
        asset.payload_type = "nuclear";
        asset.updateUser = "Basviola";
        return asset;
    }
}
