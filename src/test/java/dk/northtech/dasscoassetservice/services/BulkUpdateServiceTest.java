package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.DasscoEvent;
import dk.northtech.dasscoassetservice.domain.Funding;
import dk.northtech.dasscoassetservice.domain.InternalStatus;
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
        firstAsset.complete_digitiser_list = Arrays.asList("Bob", "Gertrud");
        assetService2.persistAsset(firstAsset, user, 1);
        assetService2.persistAsset(secondAsset, user, 1);

        Asset updatedAsset = getBulkUpdateAsset();

        Optional<Asset> optionalFirstAsset = assetService2.getAsset("bulk-asset-1");
        Optional<Asset> optionalSecondAsset = assetService2.getAsset("bulk-asset-2");
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
//
//        Optional<Asset> optionalUpdatedFirstAsset = assetService.getAsset("bulk-asset-1");
//        Optional<Asset> optionalUpdatedSecondAsset = assetService.getAsset("bulk-asset-2");
//
//        assertThat(optionalUpdatedFirstAsset.isPresent()).isTrue();
//        assertThat(optionalUpdatedSecondAsset.isPresent()).isTrue();
//
//        Asset updatedFirstAsset = optionalUpdatedFirstAsset.get();
//        Asset updatedSecondAsset = optionalUpdatedSecondAsset.get();
//
//        // Finally, the assertions:
//        // Status changed:
//        assertThat(updatedFirstAsset.status).isEqualTo("BEING_PROCESSED");
//        assertThat(updatedSecondAsset.status).isEqualTo("BEING_PROCESSED");
//        // Events changed (only one event for BULK_UPDATE_ASSET_METADATA):
//        assertThat(updatedFirstAsset.events.size()).isEqualTo(2);
//        assertThat(updatedFirstAsset.events.get(0).event).isEqualTo(DasscoEvent.BULK_UPDATE_ASSET_METADATA);
//        assertThat(updatedSecondAsset.events.size()).isEqualTo(2);
//        assertThat(updatedSecondAsset.events.get(0).event).isEqualTo(DasscoEvent.BULK_UPDATE_ASSET_METADATA);
//        // Funding changed:
//        assertThat(updatedFirstAsset.funding.get(0)).isEqualTo("Hundredetusindvis af dollars");
//        assertThat(updatedSecondAsset.funding.get(0)).isEqualTo("Hundredetusindvis af dollars");
//        // Subject changed:
//        assertThat(updatedFirstAsset.subject).isEqualTo("Folder");
//        assertThat(updatedSecondAsset.subject).isEqualTo("Folder");
//        // Payload type changed:
//        assertThat(updatedFirstAsset.payload_type).isEqualTo("nuclear");
//        assertThat(updatedSecondAsset.payload_type).isEqualTo("nuclear");
//        // Asset locked changed:
//        assertThat(updatedFirstAsset.asset_locked).isTrue();
//        assertThat(updatedSecondAsset.asset_locked).isTrue();
//        // Tags changed:
//        assertThat(updatedFirstAsset.tags.size()).isEqualTo(3);
//        assertThat(updatedSecondAsset.tags.size()).isEqualTo(3);
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
        asset.funding = Arrays.asList(new Funding("funding has depleted"));
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

        updatedAsset.status = "BEING_PROCESSED";
        updatedAsset.asset_locked = true;
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
        asset.funding = Arrays.asList(new Funding("Hundredetusindvis af dollars"));
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

}