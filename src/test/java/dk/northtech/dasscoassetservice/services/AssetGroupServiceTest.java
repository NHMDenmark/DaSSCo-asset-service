package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.A;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AssetGroupServiceTest extends AbstractIntegrationTest{

    User user = new User("Test-User");
    @Test
    void testAssetGroupsServiceUser(){
        // Creation of Asset Group as a Service User should work always:
        user.roles.add("service-user");
        Asset firstAsset = this.getTestAsset("create-asset-group-service-1");
        firstAsset.institution = "role-institution-1";
        firstAsset.collection = "role-collection-1";
        firstAsset.pipeline = "ri1_p1";
        firstAsset.workstation = "ri1_w1";
        Asset secondAsset = this.getTestAsset("create-asset-group-service-2");
        secondAsset.institution = "role-institution-1";
        secondAsset.collection = "role-collection-1";
        secondAsset.pipeline = "ri1_p1";
        secondAsset.workstation = "ri1_w1";

        assetService.persistAsset(firstAsset, user, 1);
        assetService.persistAsset(secondAsset, user, 1);

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();

        assetGroup.group_name = "test-group";
        assetGroup.assets.add(firstAsset.asset_guid);
        assetGroup.assets.add(secondAsset.asset_guid);

        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);

        assetGroupService.createAssetGroup(assetGroup, user);

        // Creation of Asset Group Assertions:
        assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(1);
        assertThat(assetGroupList.getFirst().group_name).isEqualTo(assetGroup.group_name);
        assertThat(assetGroupList.getFirst().assets.size()).isEqualTo(2);
        assertThat(assetGroupList.getFirst().assets.contains(firstAsset.asset_guid)).isTrue();
        assertThat(assetGroupList.getFirst().assets.contains(secondAsset.asset_guid)).isTrue();

        // Deletion:
        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
        assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.readAssetGroup(assetGroup.group_name, user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group does not exist!");
        user.roles.clear();
    }

    @Test
    void testAssetGroupsRole1(){
        // Creation of Asset Group as a READ_test-role-1 should work:
        user.roles.add("WRITE_test-role-1");
        Asset firstAsset = this.getTestAsset("create-asset-group-role1-1");
        firstAsset.institution = "role-institution-1";
        firstAsset.collection = "role-collection-1";
        firstAsset.pipeline = "ri1_p1";
        firstAsset.workstation = "ri1_w1";
        Asset secondAsset = this.getTestAsset("create-asset-group-role1-2");
        secondAsset.institution = "role-institution-1";
        secondAsset.collection = "role-collection-1";
        secondAsset.pipeline = "ri1_p1";
        secondAsset.workstation = "ri1_w1";

        // Need write permission to persist assets:
        assetService.persistAsset(firstAsset, user, 1);
        assetService.persistAsset(secondAsset, user, 1);

        // Read is enough for creating Asset Groups.
        user.roles.clear();
        user.roles.add("READ_test-role-1");

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();

        assetGroup.group_name = "test-group";
        assetGroup.assets.add(firstAsset.asset_guid);
        assetGroup.assets.add(secondAsset.asset_guid);

        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);

        assetGroupService.createAssetGroup(assetGroup, user);

        // Creation of Asset Group Assertions:
        assetGroupList = assetGroupService.readListAssetGroup(user);

        assertThat(assetGroupList.size()).isEqualTo(1);
        assertThat(assetGroupList.getFirst().group_name).isEqualTo(assetGroup.group_name);
        assertThat(assetGroupList.getFirst().assets.size()).isEqualTo(2);
        assertThat(assetGroupList.getFirst().assets.contains(firstAsset.asset_guid)).isTrue();
        assertThat(assetGroupList.getFirst().assets.contains(secondAsset.asset_guid)).isTrue();

        // Deletion:
        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
        assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.readAssetGroup(assetGroup.group_name, user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group does not exist!");
        user.roles.clear();
    }

    @Test
    void testAssetGroupsRole2(){
        // Creation of Asset Group as a READ_test-role-2 should work:
        user.roles.add("WRITE_test-role-2");
        Asset firstAsset = this.getTestAsset("create-asset-group-role2-1");
        firstAsset.institution = "role-institution-2";
        firstAsset.collection = "role-collection-2";
        firstAsset.pipeline = "ri2_p1";
        firstAsset.workstation = "ri2_w1";
        Asset secondAsset = this.getTestAsset("create-asset-group-role2-2");
        secondAsset.institution = "role-institution-2";
        secondAsset.collection = "role-collection-2";
        secondAsset.pipeline = "ri2_p1";
        secondAsset.workstation = "ri2_w1";

        // Need write permission to persist assets:
        assetService.persistAsset(firstAsset, user, 1);
        assetService.persistAsset(secondAsset, user, 1);

        // Read is enough for creating Asset Groups.
        user.roles.clear();
        user.roles.add("READ_test-role-2");

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();

        assetGroup.group_name = "test-group";
        assetGroup.assets.add(firstAsset.asset_guid);
        assetGroup.assets.add(secondAsset.asset_guid);

        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);

        assetGroupService.createAssetGroup(assetGroup, user);

        // Creation of Asset Group Assertions:
        assetGroupList = assetGroupService.readListAssetGroup(user);

        assertThat(assetGroupList.size()).isEqualTo(1);
        assertThat(assetGroupList.getFirst().group_name).isEqualTo(assetGroup.group_name);
        assertThat(assetGroupList.getFirst().assets.size()).isEqualTo(2);
        assertThat(assetGroupList.getFirst().assets.contains(firstAsset.asset_guid)).isTrue();
        assertThat(assetGroupList.getFirst().assets.contains(secondAsset.asset_guid)).isTrue();

        // Deletion:
        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
        assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.readAssetGroup(assetGroup.group_name, user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group does not exist!");
        user.roles.clear();
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
        Asset firstAsset = this.getTestAsset("non-existent-asset");
        firstAsset.institution = "institution_1";
        firstAsset.collection = "i1_c1";
        firstAsset.pipeline = "i1_p1";
        firstAsset.workstation = "i1_w1";

        user.roles.add("service-user");
        assetService.persistAsset(firstAsset, user, 1);

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();

        assetGroup.group_name = "test-group";
        assetGroup.assets.add(firstAsset.asset_guid);
        assetGroup.assets.add("non-existent-asset-2");

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.createAssetGroup(assetGroup, user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("One or more assets were not found!");
        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);
        user.roles.clear();
    }

    @Test
    void testCreateAssetGroupAlreadyExists(){
        Asset firstAsset = this.getTestAsset("already-exists-1");
        firstAsset.institution = "institution_1";
        firstAsset.collection = "i1_c1";
        firstAsset.pipeline = "i1_p1";
        firstAsset.workstation = "i1_w1";
        Asset secondAsset = this.getTestAsset("already-exists-2");
        secondAsset.institution = "institution_1";
        secondAsset.collection = "i1_c1";
        secondAsset.pipeline = "i1_p1";
        secondAsset.workstation = "i1_w1";

        assetService.persistAsset(firstAsset, user, 1);
        assetService.persistAsset(secondAsset, user, 1);

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();

        assetGroup.group_name = "test-group";
        assetGroup.assets.add(firstAsset.asset_guid);
        assetGroup.assets.add(secondAsset.asset_guid);

        user.roles.add("service-user");

        assetGroupService.createAssetGroup(assetGroup, user);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.createAssetGroup(assetGroup, user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group already exists!");

        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);
        user.roles.clear();
    }

    @Test
    void testCreateAssetGroupForbiddenRole1(){
        Asset firstAsset = this.getTestAsset("asset-role-3");
        firstAsset.institution = "role-institution-1";
        firstAsset.collection = "role-collection-1";
        firstAsset.pipeline = "ri1_p1";
        firstAsset.workstation = "ri1_w1";
        Asset secondAsset = this.getTestAsset("asset-role-4");
        secondAsset.institution = "role-institution-2";
        secondAsset.collection = "role-collection-2";
        secondAsset.pipeline = "ri2_p1";
        secondAsset.workstation = "ri2_w1";

        user.roles.add("service-user");
        assetService.persistAsset(firstAsset, user, 1);
        assetService.persistAsset(secondAsset, user, 1);

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();

        assetGroup.group_name = "test-group";
        assetGroup.assets.add(firstAsset.asset_guid);
        assetGroup.assets.add(secondAsset.asset_guid);

        user.roles.clear();
        user.roles.add("READ_test-role-2");
        assertThrows(DasscoIllegalActionException.class, () -> assetGroupService.createAssetGroup(assetGroup, user));
        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);

        user.roles.clear();
    }

    @Test
    void testCreateAssetGroupForbiddenRole2(){
        Asset firstAsset = this.getTestAsset("asset-role-1");
        firstAsset.institution = "role-institution-1";
        firstAsset.collection = "role-collection-1";
        firstAsset.pipeline = "ri1_p1";
        firstAsset.workstation = "ri1_w1";
        Asset secondAsset = this.getTestAsset("asset-role-2");
        secondAsset.institution = "role-institution-2";
        secondAsset.collection = "role-collection-2";
        secondAsset.pipeline = "ri2_p1";
        secondAsset.workstation = "ri2_w1";

        user.roles.add("service-user");
        assetService.persistAsset(firstAsset, user, 1);
        assetService.persistAsset(secondAsset, user, 1);

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();

        assetGroup.group_name = "test-group";
        assetGroup.assets.add(firstAsset.asset_guid);
        assetGroup.assets.add(secondAsset.asset_guid);

        user.roles.clear();
        user.roles.add("READ_test-role-1");
        assertThrows(DasscoIllegalActionException.class, () -> assetGroupService.createAssetGroup(assetGroup, user));
        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);

        user.roles.clear();
    }

    @Test
    void testReadAssetGroupServiceUser(){
        Asset firstAsset = this.getTestAsset("readGroup-service");
        firstAsset.institution = "role-institution-1";
        firstAsset.collection = "role-collection-1";
        firstAsset.pipeline = "ri1_p1";
        firstAsset.workstation = "ri1_w1";

        user.roles.add("service-user");
        assetService.persistAsset(firstAsset, user, 1);

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();

        assetGroup.group_name = "test-group";
        assetGroup.assets.add(firstAsset.asset_guid);

        assetGroupService.createAssetGroup(assetGroup, user);

        List<Asset> found = assetGroupService.readAssetGroup(assetGroup.group_name, user);
        assertThat(found.size()).isEqualTo(1);

        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);
        user.roles.clear();
    }

    @Test
    void testReadAssetGroupForbidden(){
        Asset firstAsset = this.getTestAsset("readGroup-role1");
        firstAsset.institution = "role-institution-1";
        firstAsset.collection = "role-collection-1";
        firstAsset.pipeline = "ri1_p1";
        firstAsset.workstation = "ri1_w1";

        user.roles.add("service-user");
        assetService.persistAsset(firstAsset, user, 1);

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();

        assetGroup.group_name = "test-group";
        assetGroup.assets.add(firstAsset.asset_guid);

        user.roles.clear();
        user.roles.add("READ_test-role-1");

        assetGroupService.createAssetGroup(assetGroup, user);

        List<Asset> found = assetGroupService.readAssetGroup(assetGroup.group_name, user);
        assertThat(found.size()).isEqualTo(1);

        user.roles.clear();
        user.roles.add("READ_test-role-2");
        assertThrows(DasscoIllegalActionException.class, () -> assetGroupService.readAssetGroup(assetGroup.group_name, user));

        user.roles.clear();
        user.roles.add("READ_test-role-1");

        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);
        user.roles.clear();
    }


    @Test
    void testReadAssetGroupGroupDoesNotExist(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.readAssetGroup("this-asset-does-not-exist", user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group does not exist!");
    }

    @Test
    void readListAssetGroupServiceUser(){
        Asset firstAsset = this.getTestAsset("readList-service");
        firstAsset.institution = "role-institution-1";
        firstAsset.collection = "role-collection-1";
        firstAsset.pipeline = "ri1_p1";
        firstAsset.workstation = "ri1_w1";

        Asset secondAsset = this.getTestAsset("readList-service-2");
        secondAsset.institution = "role-institution-2";
        secondAsset.collection = "role-collection-2";
        secondAsset.pipeline = "ri2_p1";
        secondAsset.workstation = "ri2_w1";

        user.roles.add("service-user");
        assetService.persistAsset(firstAsset, user, 1);
        assetService.persistAsset(secondAsset, user, 1);

        AssetGroup assetGroup = new AssetGroup();
        AssetGroup secondAssetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();
        secondAssetGroup.assets = new ArrayList<>();

        assetGroup.group_name = "test-group";
        secondAssetGroup.group_name = "test-group-2";
        assetGroup.assets.add(firstAsset.asset_guid);
        secondAssetGroup.assets.add(secondAsset.asset_guid);

        assetGroupService.createAssetGroup(assetGroup, user);
        assetGroupService.createAssetGroup(secondAssetGroup, user);

        List<AssetGroup> found = assetGroupService.readListAssetGroup(user);
        assertThat(found.size()).isEqualTo(2);

        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
        assetGroupService.deleteAssetGroup(secondAssetGroup.group_name, user);
        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);
        user.roles.clear();
    }

    @Test
    void readListAssetGroupRole(){
        Asset firstAsset = this.getTestAsset("readList-role1");
        firstAsset.institution = "role-institution-1";
        firstAsset.collection = "role-collection-1";
        firstAsset.pipeline = "ri1_p1";
        firstAsset.workstation = "ri1_w1";

        Asset secondAsset = this.getTestAsset("readList-role2");
        secondAsset.institution = "role-institution-2";
        secondAsset.collection = "role-collection-2";
        secondAsset.pipeline = "ri2_p1";
        secondAsset.workstation = "ri2_w1";

        user.roles.add("service-user");
        assetService.persistAsset(firstAsset, user, 1);
        assetService.persistAsset(secondAsset, user, 1);

        AssetGroup assetGroup = new AssetGroup();
        AssetGroup secondAssetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();
        secondAssetGroup.assets = new ArrayList<>();

        assetGroup.group_name = "test-group";
        secondAssetGroup.group_name = "test-group-2";
        assetGroup.assets.add(firstAsset.asset_guid);
        secondAssetGroup.assets.add(secondAsset.asset_guid);

        assetGroupService.createAssetGroup(assetGroup, user);
        assetGroupService.createAssetGroup(secondAssetGroup, user);

        user.roles.clear();
        user.roles.add("READ_test-role-1");

        List<AssetGroup> found = assetGroupService.readListAssetGroup(user);
        assertThat(found.size()).isEqualTo(1);
        assertThat(assetGroupService.readAssetGroup(assetGroup.group_name, user)).isNotEmpty();
        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
        assertThrows(DasscoIllegalActionException.class, () ->assetGroupService.deleteAssetGroup(secondAssetGroup.group_name, user));
        user.roles.clear();
        user.roles.add("READ_test-role-2");
        List<AssetGroup> secondFound = assetGroupService.readListAssetGroup(user);
        assertThat(found.size()).isEqualTo(1);
        assertThat(assetGroupService.readAssetGroup(secondAssetGroup.group_name, user)).isNotEmpty();
        assetGroupService.deleteAssetGroup(assetGroup.group_name, user);
        assetGroupService.deleteAssetGroup(secondAssetGroup.group_name, user);
        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup(user);
        assertThat(assetGroupList.size()).isEqualTo(0);
        user.roles.clear();
        // TODO: There's something wrong with the query, it is returning every asset group, even those that should not be returned, based on the roles!
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
