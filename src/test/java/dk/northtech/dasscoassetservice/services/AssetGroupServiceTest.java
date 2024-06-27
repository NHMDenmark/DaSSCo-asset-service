package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AssetGroupServiceTest extends AbstractIntegrationTest{

    User user = new User("Test-User");
    @Test
    void testAssetGroups(){
        Asset firstAsset = this.getTestAsset("create-asset-group-1");
        Asset secondAsset = this.getTestAsset("create-asset-group-2");

        assetService.persistAsset(firstAsset, user, 1);
        assetService.persistAsset(secondAsset, user, 1);

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();

        assetGroup.group_name = "test-group";
        assetGroup.assets.add(firstAsset.asset_guid);
        assetGroup.assets.add(secondAsset.asset_guid);

        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup();
        assertThat(assetGroupList.size()).isEqualTo(0);

        assetGroupService.createAssetGroup(assetGroup);

        // Creation of Asset Group Assertions:
        assetGroupList = assetGroupService.readListAssetGroup();
        assertThat(assetGroupList.size()).isEqualTo(1);
        assertThat(assetGroupList.getFirst().group_name).isEqualTo(assetGroup.group_name);
        assertThat(assetGroupList.getFirst().assets.size()).isEqualTo(2);
        assertThat(assetGroupList.getFirst().assets.contains(firstAsset.asset_guid)).isTrue();
        assertThat(assetGroupList.getFirst().assets.contains(secondAsset.asset_guid)).isTrue();

        // Deletion:
        assetGroupService.deleteAssetGroup(assetGroup.group_name);
        assetGroupList = assetGroupService.readListAssetGroup();
        assertThat(assetGroupList.size()).isEqualTo(0);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.readAssetGroup(assetGroup.group_name));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group does not exist!");
    }

    @Test
    void testCreateAssetGroupNoName(){
        AssetGroup assetGroup = new AssetGroup();
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.createAssetGroup(assetGroup));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group needs a name!");
        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup();
        assertThat(assetGroupList.size()).isEqualTo(0);
    }

    @Test
    void testCreateAssetGroupNoAssets(){
        AssetGroup assetGroup = new AssetGroup();
        assetGroup.group_name = "asset-group-no-assets";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.createAssetGroup(assetGroup));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group needs assets!");
        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup();
        assertThat(assetGroupList.size()).isEqualTo(0);
    }

    @Test
    void testCreateAssetGroupNonExistentAssets() {
        Asset firstAsset = this.getTestAsset("non-existent-asset");

        assetService.persistAsset(firstAsset, user, 1);

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();

        assetGroup.group_name = "test-group";
        assetGroup.assets.add(firstAsset.asset_guid);
        assetGroup.assets.add("non-existent-asset-2");

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.createAssetGroup(assetGroup));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("One or more assets were not found!");
        List<AssetGroup> assetGroupList = assetGroupService.readListAssetGroup();
        assertThat(assetGroupList.size()).isEqualTo(0);
    }

    @Test
    void testCreateAssetAlreadyExists(){
        Asset firstAsset = this.getTestAsset("already-exists-1");
        Asset secondAsset = this.getTestAsset("already-exists-2");

        assetService.persistAsset(firstAsset, user, 1);
        assetService.persistAsset(secondAsset, user, 1);

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();

        assetGroup.group_name = "test-group";
        assetGroup.assets.add(firstAsset.asset_guid);
        assetGroup.assets.add(secondAsset.asset_guid);

        assetGroupService.createAssetGroup(assetGroup);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.createAssetGroup(assetGroup));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group already exists!");
    }

    @Test
    void testReadAssetGroupGroupDoesNotExist(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetGroupService.readAssetGroup("this-asset-does-not-exist"));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset group does not exist!");
    }

    @Test
    void testUpdateAssetGroup(){
        Asset firstAsset = this.getTestAsset("update-asset-group-1");
        Asset secondAsset = this.getTestAsset("update-asset-group-2");
        Asset thirdAsset = this.getTestAsset("update-asset-group-3");

        assetService.persistAsset(firstAsset, user, 1);
        assetService.persistAsset(secondAsset, user, 1);
        assetService.persistAsset(thirdAsset, user, 1);

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.assets = new ArrayList<>();

        assetGroup.group_name = "update-group";
        assetGroup.assets.add(firstAsset.asset_guid);
        assetGroup.assets.add(secondAsset.asset_guid);
        assetGroup.assets.add(thirdAsset.asset_guid);

        assetGroupService.createAssetGroup(assetGroup);
        List<Asset> assets = assetGroupService.readAssetGroup(assetGroup.group_name);
        assertThat(assets.size()).isEqualTo(3);
        // Assert that the assets are the three that we have.
        // Then update and check that the one we removed is not there but the other two are.
        // Etc.
    }

    public Asset getTestAsset(String guid) {
        Asset asset = new Asset();
        asset.asset_pid = guid + "-pid";
        asset.status = AssetStatus.BEING_PROCESSED;
        asset.pipeline = "i1_p1";
        asset.workstation = "i1_w1";
        asset.institution = "institution_1";
        asset.collection = "i1_c1";
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
