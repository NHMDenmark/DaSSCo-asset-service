package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class BulkUpdateServiceTest extends AbstractIntegrationTest{
    @Test
    void test() {

    }

    @Test
    void testBulkUpdate(){
        // Create three different assets
        Asset firstAsset = getBulkUpdateAssetToBeUpdated("bulk-asset-1");
        Asset secondAsset = getBulkUpdateAssetToBeUpdated("bulk-asset-2");
        Asset thirdAsset = getBulkUpdateAssetToBeUpdated("bulk-asset-3");
        firstAsset.complete_digitiser_list = Arrays.asList("Bob", "Gertrud");
        assetService.persistAsset(firstAsset, user, 1);
        assetService.persistAsset(secondAsset, user, 1);
        assetService.persistAsset(thirdAsset, user, 1);

        Asset updatedAsset = getBulkUpdateAsset();

        Optional<Asset> optionalFirstAsset = assetService.getAsset("bulk-asset-1");
        Optional<Asset> optionalSecondAsset = assetService.getAsset("bulk-asset-2");
        assertThat(optionalFirstAsset.isPresent()).isTrue();
        assertThat(optionalSecondAsset.isPresent()).isTrue();
        Asset persistedFirstAsset = optionalFirstAsset.get();
        Asset persistedSecondAsset = optionalSecondAsset.get();

        assertThat(persistedFirstAsset.events.size()).isEqualTo(1);
        assertThat(persistedFirstAsset.events.get(0).event).isEqualTo(DasscoEvent.CREATE_ASSET_METADATA);
        assertThat(persistedSecondAsset.events.size()).isEqualTo(1);
        assertThat(persistedSecondAsset.events.get(0).event).isEqualTo(DasscoEvent.CREATE_ASSET_METADATA);

        // Create list of assets to be updated:
        List<String> assetList = new ArrayList<>();
        assetList.add("bulk-asset-1");
        assetList.add("bulk-asset-2");
        // Update assets with the new asset information:
        bulkUpdateService.bulkUpdate(assetList, updatedAsset, user);

        Optional<Asset> optionalUpdatedFirstAsset = assetService.getAsset("bulk-asset-1");
        Optional<Asset> optionalUpdatedSecondAsset = assetService.getAsset("bulk-asset-2");
        Optional<Asset> optionalUnchangedThirdASset = assetService.getAsset("bulk-asset-3");
        assertThat(optionalUpdatedFirstAsset.isPresent()).isTrue();
        assertThat(optionalUpdatedSecondAsset.isPresent()).isTrue();

        Asset updatedFirstAsset = optionalUpdatedFirstAsset.get();
        Asset updatedSecondAsset = optionalUpdatedSecondAsset.get();
        Asset unChangedThirdAsset = optionalUnchangedThirdASset.orElseThrow();

        // Finally, the assertions:
        // Status changed:
        assertThat(updatedFirstAsset.status).isEqualTo("BEING_PROCESSED");
        assertThat(updatedSecondAsset.status).isEqualTo("BEING_PROCESSED");
        assertThat(unChangedThirdAsset.status).isEqualTo("WORKING_COPY");
        // Events changed (only one event for BULK_UPDATE_ASSET_METADATA):
        assertThat(updatedFirstAsset.events.size()).isEqualTo(2);
        assertThat(updatedFirstAsset.events.get(0).event).isEqualTo(DasscoEvent.BULK_UPDATE_ASSET_METADATA);
        assertThat(updatedSecondAsset.events.size()).isEqualTo(2);
        assertThat(updatedSecondAsset.events.get(0).event).isEqualTo(DasscoEvent.BULK_UPDATE_ASSET_METADATA);
        // Funding changed:
        assertThat(updatedFirstAsset.funding).contains("Hundredetusindvis af dollars");
        assertThat(updatedSecondAsset.funding).contains("Hundredetusindvis af dollars");
        assertThat(unChangedThirdAsset.funding).hasSize(2);
        // Subject changed:
        assertThat(updatedFirstAsset.subject).isEqualTo("Folder");
        assertThat(updatedSecondAsset.subject).isEqualTo("Folder");
        // Payload type changed:
        assertThat(updatedFirstAsset.payload_type).isEqualTo("nuclear");
        assertThat(updatedSecondAsset.payload_type).isEqualTo("nuclear");
        // Asset locked changed:
        assertThat(updatedFirstAsset.asset_locked).isTrue();
        assertThat(updatedSecondAsset.asset_locked).isTrue();
        // Tags changed:
        assertThat(updatedFirstAsset.tags.size()).isEqualTo(3);
        assertThat(updatedSecondAsset.tags.size()).isEqualTo(3);
//        while(true) {
//            try {
//                Thread.sleep(10000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
    }

    public Asset getBulkUpdateAssetToBeUpdated(String guid){
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.pipeline = "i2_p1";
        asset.workstation = "i2_w1";
        asset.collection = "i2_c1";
        asset.asset_pid = "pid-" + guid;
        asset.asset_guid = guid;
        asset.status = "WORKING_COPY";
        asset.funding = Arrays.asList("funding has depleted", "Dollaridooz");
        asset.subject = "subject-non-edited";
        asset.payload_type = "payload-not-edited";
        asset.tags.put("Tag 1", "Value 1");
        asset.tags.put("Tag 2", "Value 2");
        asset.digitiser = "test-user";
        asset.updateUser = "update-user";

        return asset;
    }

    public Asset getBulkUpdateAsset(){
        Asset updatedAsset = getTestAsset("updatedAsset");
        updatedAsset.institution = "institution_2";
        updatedAsset.pipeline = "i2_p1";
        updatedAsset.workstation = "i2_w1";
        updatedAsset.collection = "i2_c1";
        updatedAsset.funding.add("A million billion dollaz");
        updatedAsset.funding.add("Dark money");
        updatedAsset.status = "BEING_PROCESSED";
        updatedAsset.asset_locked = true;
        updatedAsset.subject = "New subject";
        updatedAsset.tags.put("Tag 3", "Value 3");
        return updatedAsset;
    }

    public Asset getTestAsset(String guid) {
        Asset asset = new Asset();
        asset.asset_locked = false;
        asset.status = "BEING_PROCESSED";
        asset.digitiser = "Karl-Børge";
        asset.asset_guid = guid;
        asset.asset_pid = guid + "_pid";
        asset.funding.add("Hundredetusindvis af dollars");
        asset.date_asset_taken = Instant.now();
        asset.subject = "Folder";
        asset.file_formats = Arrays.asList("JPEG");
        asset.payload_type = "nuclear";
        asset.updateUser = "Basviola";
        asset.institution = "institution_2";
        asset.workstation = "i2_w1";
        asset.pipeline = "i2_p1";
        asset.collection = "i2_c1";
        asset.camera_setting_control = "Mom get the camera!";
        asset.date_asset_finalised = Instant.now();
        asset.metadata_source = "I made it up";
        asset.metadata_version = "1.0.0";
        asset.date_metadata_ingested = Instant.now();
        asset.internal_status = InternalStatus.ASSET_RECEIVED;
        asset.make_public = true;
        asset.push_to_specify = true;
        return asset;
    }

    @Test
    void testBulkUpdateNoBody(){
        List<String> assetList = new ArrayList<String>();
        assetList.add("bulk-asset-no-body");
        assetList.add("bulk-asset-no-body-2");
        Asset updatedAsset = null;
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> bulkUpdateService.bulkUpdate(assetList, updatedAsset,user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Empty body, please specify fields to update");
    }

    @Test
    void testBulkUpdateOneAssetNotFound(){
        // Create three different assets
        Asset firstAsset = getBulkUpdateAssetToBeUpdated("bulk-asset-exists");
        Asset secondAsset = getBulkUpdateAssetToBeUpdated("bulk-asset-does-not");

        assetService.persistAsset(firstAsset, user, 1);

        Asset updatedAsset = getBulkUpdateAsset();

        // Create list of assets to be updated:
        List<String> assetList = new ArrayList<String>();
        assetList.add("bulk-asset-exists");
        assetList.add("bulk-asset-does-not");
        // Update assets with the new asset information:
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> bulkUpdateService.bulkUpdate(assetList, updatedAsset,user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("One or more assets were not found!");
    }

    @Test
    void testBulkUpdateNoAssetParent(){

        Asset toBeUpdated = getBulkUpdateAssetToBeUpdated("bulk-update-no-parent");
        Asset secondToBeUpdated = getBulkUpdateAssetToBeUpdated("bulk-update-no-parent-2");
        assetService.persistAsset(toBeUpdated, user, 1);
        assetService.persistAsset(secondToBeUpdated, user, 1);

        Asset updatedAsset = getBulkUpdateAsset();
        updatedAsset.parent_guid = "this-does-not-exist";

        List<String> listOfAssets = Arrays.asList("bulk-update-no-parent", "bulk-update-no-parent-2");

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> bulkUpdateService.bulkUpdate(listOfAssets, updatedAsset,user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("asset_parent does not exist!");

        Optional<Asset> optNotUpdated = assetService.getAsset("bulk-update-no-parent-2");
        assertThat(optNotUpdated.isPresent()).isTrue();
        Asset notUpdated = optNotUpdated.get();
        // Using funding or subject or payload_type is the easiest way to check if an asset was updated or not.
        assertThat(notUpdated.funding).contains("funding has depleted");
    }

    @Test
    void testBulkUpdateNoUnlockAllowed(){

        Asset firstToBeUpdated = getBulkUpdateAssetToBeUpdated("bulk-update-no-unlocking-allowed");
        Asset secondToBeUpdated = getBulkUpdateAssetToBeUpdated("bulk-update-no-unlocking-allowed-2");
        secondToBeUpdated.asset_locked = true;
        assetService.persistAsset(firstToBeUpdated, user, 1);
        assetService.persistAsset(secondToBeUpdated, user, 1);

        Asset updatedAsset = getBulkUpdateAsset();
        updatedAsset.asset_locked = false;

        List<String> assetList = Arrays.asList("bulk-update-no-unlocking-allowed", "bulk-update-no-unlocking-allowed-2");

        DasscoIllegalActionException dasscoIllegalActionException = assertThrows(DasscoIllegalActionException.class, () -> bulkUpdateService.bulkUpdate(assetList, updatedAsset,user));
        assertThat(dasscoIllegalActionException).hasMessageThat().isEqualTo("Cannot unlock using updateAsset API, use dedicated API for unlocking");

        Optional<Asset> optAsset = assetService.getAsset("bulk-update-no-unlocking-allowed");
        assertThat(optAsset.isPresent()).isTrue();
        Asset found = optAsset.get();
        assertThat(found.asset_locked).isFalse();
        Optional<Asset> optAsset2 = assetService.getAsset("bulk-update-no-unlocking-allowed-2");
        assertThat(optAsset2.isPresent()).isTrue();
        Asset found2 = optAsset2.get();
        assertThat(found2.asset_locked).isTrue();
    }

    @Test
    void testBulkUpdateNoUpdateUser(){

        Asset asset = new Asset();
        List<String> assetList = new ArrayList<>();
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> bulkUpdateService.bulkUpdate(assetList, asset,user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Update user must be provided!");
    }
}