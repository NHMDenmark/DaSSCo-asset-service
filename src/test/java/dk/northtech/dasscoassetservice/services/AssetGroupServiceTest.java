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

    User user = new User("Test-User");

    @Test
    void testCreateAssetGroupServiceUserNotShared(){
        // Creation of Asset Group as a Service User should work always:
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
        user.roles.clear();
    }

    @Test
    void testCreateAssetGroupServiceUserShared(){
        // Creation of Asset Group as a Service User should work always:
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
        user.roles.clear();
    }

    @Test
    void testAssetGroupsRole1ReadNotShared(){
        // Creation of Asset Group as a READ_role-1 should work:
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
        user.roles.clear();
    }

    @Test
    void testAssetGroupsRole1WriteShared(){
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
        user.roles.clear();
    }

    @Test
    void failTestCreateAssetRole1ReadShared(){
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
        user.roles.add("fail");
        AssetGroup assetGroup = new AssetGroup();
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.createAssetGroup(assetGroup, user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group needs a name!");
        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);
        user.roles.clear();
    }

    @Test
    void testCreateAssetGroupNoAssets(){
        user.roles.add("fail");
        AssetGroup assetGroup = new AssetGroup();
        assetGroup.group_name = "asset-group-no-assets";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.createAssetGroup(assetGroup, user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group needs assets!");
        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);
        user.roles.clear();
    }

    @Test
    void testCreateAssetGroupNonExistentAssets() {
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
        user.roles.clear();
    }

    @Test
    void testCreateAssetGroupAlreadyExists(){

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

        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
        assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(3);
        user.roles.clear();
    }


    @Test
    void testReadAssetGroupServiceUser(){
        user.roles.add("service-user");

        List<Asset> found = assetGroupService.readAssetGroup("ag-1", user);
        assertThat(found.size()).isEqualTo(3);

        user.roles.clear();
    }

    @Test
    void testReadAssetGroupRole1(){
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
        user.roles.clear();
    }


    @Test
    void testReadAssetGroupForbidden(){
        user.roles.add("READ_role-2");
        assertThrows(DasscoIllegalActionException.class, () -> assetGroupService.readAssetGroup("ag-1", user));

        user.roles.clear();
    }

    @Test
    void testReadAssetGroupGroupDoesNotExist(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.readAssetGroup("this-asset-does-not-exist", user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group does not exist!");
    }


    @Test
    void readListAssetGroupServiceUser(){
        user.roles.add("service-user");

        List<AssetGroup> found = assetGroupService.readListAssetGroup(user);
        assertThat(found.size()).isEqualTo(3);

        user.roles.clear();
    }

    @Test
    void readListAssetGroupRole1(){
        User newUser = new User("role-1-user");
        List<AssetGroup> found = assetGroupService.readListAssetGroup(newUser);
        assertThat(found.size()).isEqualTo(1);
        user.roles.clear();
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
