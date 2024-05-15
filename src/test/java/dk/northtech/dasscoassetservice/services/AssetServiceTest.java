package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.webapi.domain.HttpAllocationStatus;
import dk.northtech.dasscoassetservice.webapi.domain.HttpInfo;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AssetServiceTest extends AbstractIntegrationTest {

//    @Inject
//    AssetService assetService;
//


    User user = new User("Teztuzer");
    @Test
    void createAsset() {
        Asset createAsset = getTestAsset("createAsset");
        createAsset.pipeline = "i1_p1";
        createAsset.workstation = "i1_w1";
        createAsset.tags.put("Tag1", "value1");
        createAsset.tags.put("Tag2", "value2");
        createAsset.institution = "institution_1";
        createAsset.specimens = Arrays.asList(new Specimen(createAsset.institution, "i1_c1", "creatAsset-sp-1", "spid1", "slide"), new Specimen(createAsset.institution, "i1_c1", "creatAsset-sp-2", "spid2", "pinning"));
        createAsset.collection = "i1_c1";
        createAsset.asset_pid = "pid-createAsset";
        createAsset.status = AssetStatus.BEING_PROCESSED;
        assetService.persistAsset(createAsset, user, 10);
        //Check that the same asset cannot be added multiple times
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.persistAsset(createAsset, user, 11));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset createAsset already exists");
        Optional<Asset> resultOpt = assetService.getAsset("createAsset");
        assertThat(resultOpt.isPresent()).isTrue();
        Asset result = resultOpt.get();
        assertThat(result.pipeline).isEqualTo("i1_p1");
        assertThat(result.workstation).isEqualTo("i1_w1");
        assertThat(result.collection).isEqualTo("i1_c1");
        assertThat(result.tags.get("Tag1")).isEqualTo("value1");
        assertThat(result.tags.get("Tag2")).isEqualTo("value2");
        assertThat(result.institution).isEqualTo("institution_1");
        assertThat(result.digitiser).isEqualTo("Karl-Børge");
        assertThat(result.internal_status).isEqualTo(InternalStatus.METADATA_RECEIVED);
        assertThat(result.parent_guid).isNull();
//        assertThat(result.specimen_barcodes).contains("createAsset-sp-1");
//        assertThat(result.specimen_barcodes).contains("createAsset-sp-2");
        assertThat(result.payload_type).isEqualTo("nuclear");
        assertThat(result.funding).isEqualTo("Hundredetusindvis af dollars");
        //Specimens
        assertThat(result.specimens).hasSize(2);
        Specimen specimen_1 = result.specimens.get(0).barcode().equals("creatAsset-sp-1") ? result.specimens.get(0): result.specimens.get(1);
        assertThat(specimen_1.barcode()).isEqualTo("creatAsset-sp-1");
        assertThat(specimen_1.specimen_pid()).isEqualTo("spid1");
        assertThat(specimen_1.preparation_type()).isEqualTo("slide");
        Specimen specimen_2 = result.specimens.get(0).barcode().equals("creatAsset-sp-2") ? result.specimens.get(0): result.specimens.get(1);
        assertThat(specimen_2.barcode()).isEqualTo("creatAsset-sp-2");
        assertThat(specimen_2.specimen_pid()).isEqualTo("spid2");
        assertThat(specimen_2.preparation_type()).isEqualTo("pinning");
    }

    //We have had some troubles with reading null values from the database this test should give error if any of the nullable fields cause null pointers
    @Test
    void createAssetUpdateWithMaxNull() {
        Asset createAsset = getTestAsset("createAssetUpdateWithMaxNull");
        createAsset.pipeline = "i1_p1";
        createAsset.workstation = "i1_w1";
        createAsset.tags.put("Tag1", "value1");
        createAsset.tags.put("Tag2", "value2");
        createAsset.institution = "institution_1";
        createAsset.collection = "i1_c1";
        createAsset.asset_pid = "pid-createAsset";
        createAsset.status = AssetStatus.BEING_PROCESSED;
        assetService.persistAsset(createAsset, user,10);
        Asset asset = new Asset();
        asset.pipeline = "i1_p1";
        asset.workstation = "i1_w1";
        asset.institution = "institution_1";
        asset.collection = "i1_c1";
        asset.asset_pid = "createAssetUpdateWithMaxNull_pid";
        asset.asset_guid = "createAssetUpdateWithMaxNull";
        asset.updateUser = "thbo";
        asset.status = AssetStatus.ISSUE_WITH_METADATA;
        assetService.updateAsset(asset);
        Optional<Asset> resultOpt = assetService.getAsset("createAssetUpdateWithMaxNull");
        assertThat(resultOpt.isPresent()).isTrue();
    }
    //We have had some troubles with reading null values from the database this test should give error if any of the nullable fields cause null pointers
    @Test
    void createAssetMaxNulls() {
        Asset createAsset = new Asset();
        createAsset.pipeline = "i1_p1";
        createAsset.workstation = "i1_w1";
        createAsset.institution = "institution_1";
        createAsset.collection = "i1_c1";
        createAsset.asset_pid = "createAssetMaxNulls_pid";
        createAsset.asset_guid = "createAssetMaxNulls";
        createAsset.updateUser = "thbo";
        createAsset.status = AssetStatus.BEING_PROCESSED;
        assetService.persistAsset(createAsset, user,10);
        assetService.updateAsset(createAsset);
        Optional<Asset> resultOpt = assetService.getAsset("createAssetMaxNulls");
        assertThat(resultOpt.isPresent()).isTrue();
    }

//    @Test
//    void testParentRestricted() {
//        Asset createAsset = getTestAsset("testParentRestricted");
//        createAsset.specimen_barcodes = Arrays.asList("testParentRestricted-sp-1");
//        createAsset.pipeline = "i1_p1";
//        createAsset.workstation = "i1_w1";
//        createAsset.tags.put("Tag1", "value1");
//        createAsset.tags.put("Tag2", "value2");
//        createAsset.institution = "institution_1";
//        createAsset.collection = "i1_c1";
//        createAsset.pid = "pid-createAsset";
//        createAsset.status = AssetStatus.BEING_PROCESSED;
//        createAsset.restricted_access = Arrays.asList(Role.SERVICE_USER);
//        assetService.persistAsset(createAsset, user);
////        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.persistAsset(createAsset, user));
//        Asset child = getTestAsset("testParentRestricted_child");
//        User user = new User();
//        user.roles = new HashSet<>(Arrays.asList("dassco-user"));
//        child.parent_guid = createAsset.asset_guid;
//        DasscoIllegalActionException illegalArgumentException = assertThrows(DasscoIllegalActionException.class, () -> assetService.persistAsset(createAsset, user));
//        assertThat(illegalArgumentException.getCause()).isEqualTo("parent is restricted");
//    }

    @Test
    void deleteAsset() {
        Asset createAsset = getTestAsset("deleteAsset");
        createAsset.pipeline = "i1_p1";
        createAsset.workstation = "i1_w1";
        createAsset.tags.put("Tag1", "value1");
        createAsset.tags.put("Tag2", "value2");
        createAsset.institution = "institution_1";
        createAsset.collection = "i1_c1";
        createAsset.asset_pid = "pid-createAsset";
        createAsset.status = AssetStatus.BEING_PROCESSED;
        assetService.persistAsset(createAsset, user,10);
        assetService.deleteAsset("deleteAsset", user);
        Optional<Asset> deleteAssetOpt = assetService.getAsset("deleteAsset");
        Asset result = deleteAssetOpt.get();
        assertThat(result.date_asset_deleted).isNotNull();
    }

    @Test
    void createAssetUpdateAsset() {
        Asset createAsset = getTestAsset("createAssetUpdateAsset");
        createAsset.specimens = Arrays.asList(new Specimen(createAsset.institution, "i1_c1", "creatAsset-sp-1", "spid1", "slide"));
        createAsset.pipeline = "i1_p1";
        createAsset.workstation = "i1_w1";
        createAsset.tags.put("Tag1", "value1");
        createAsset.tags.put("Tag2", "value2");
        createAsset.institution = "institution_1";
        createAsset.collection = "i1_c1";
        createAsset.asset_pid = "pid-createAsset";
        createAsset.status = AssetStatus.BEING_PROCESSED;
        assetService.persistAsset(createAsset, user,10);
        Optional<Asset> resultOpt = assetService.getAsset("createAssetUpdateAsset");
        assertThat(resultOpt.isPresent()).isTrue();
        Asset result = resultOpt.get();
        result.updateUser = "Uffe Updater";
        result.payload_type = "conventional";
        assetService.updateAsset(result);
        result.payload_type = "nuclear";
        assetService.updateAsset(result);
        assetService.completeAsset(new AssetUpdateRequest(null, new MinimalAsset("createAssetUpdateAsset", null, null, null),"i1_w1", "i1_p1", "bob"));
        assetService.auditAsset(new Audit("Audrey Auditor"), "createAssetUpdateAsset");
        List<Event> resultEvents = assetService.getEvents(result.asset_guid);
        assertThat(resultEvents.size()).isEqualTo(5);
        Optional<Asset> resultOpt2 = assetService.getAsset("createAssetUpdateAsset");
        Asset resultAsset = resultOpt2.get();
        assertThat(resultAsset.payload_type).isEqualTo("nuclear");
        Instant latestUpdate;
        List<Instant> updates = resultEvents.stream()
                .filter(x -> x.event.equals(DasscoEvent.UPDATE_ASSET_METADATA))
                .map(x -> x.timeStamp)
                .sorted().toList();
        //The last update event
        assertThat(resultAsset.date_metadata_updated).isEqualTo(updates.get(1));
        assertThat(resultAsset.audited).isTrue();
    }

    @Test
    void doNotPermitUnlocking() {
        Asset createAsset = getTestAsset("doNotPermitUnlocking");
        createAsset.pipeline = "i1_p1";
        createAsset.workstation = "i1_w1";
        createAsset.tags.put("Tag1", "value1");
        createAsset.tags.put("Tag2", "value2");
        createAsset.institution = "institution_1";
        createAsset.collection = "i1_c1";
        createAsset.asset_pid = "pid-createAsset";
        createAsset.status = AssetStatus.BEING_PROCESSED;
        createAsset = assetService.persistAsset(createAsset, user, 11);

        createAsset.updateUser = "Uffe Updater";
        createAsset.asset_locked = true;
        assetService.updateAsset(createAsset);
        createAsset.asset_locked = false;
        Asset finalCreateAsset = createAsset;
        DasscoIllegalActionException illegalActionException = assertThrows(DasscoIllegalActionException.class, () -> assetService.updateAsset(finalCreateAsset));
        assertThat(illegalActionException.getMessage()).isEqualTo("Cannot unlock using updateAsset API, use dedicated API for unlocking");

    }

    @Test
    void updateAsset() {
        Asset asset = getTestAsset("updateAsset");
//        asset.specimen_barcodes = Arrays.asList("createAsset-sp-1", "createAsset-sp-2");
        asset.institution = "institution_1";
        asset.specimens = new ArrayList<>(Arrays.asList(new Specimen(asset.institution, "i1_c1", "creatAsset-sp-1", "spid1", "slide")
                , new Specimen(asset.institution, "i1_c1", "creatAsset-sp-2", "spid1", "pinning")));
        asset.pipeline = "i1_p1";
        asset.workstation = "i1_w1";
        asset.tags.put("Tag1", "value1");
        asset.tags.put("Tag2", "value2");
        asset.collection = "i1_c1";
        asset.asset_pid = "pid-updateAsset";
        asset.status = AssetStatus.BEING_PROCESSED;
        assetService.persistAsset(asset, user, 11);
        asset.tags.remove("Tag1");
        asset.tags.remove("Tag2");
        asset.specimens = List.of(new Specimen(asset.institution, asset.collection, "creatAsset-sp-2", "spid2", "slide"));
//        asset.specimens.get()
        asset.workstation = "i1_w2";
        asset.pipeline = "i1_p2";
        asset.status = AssetStatus.ISSUE_WITH_METADATA;
        asset.subject = "new sub";
        asset.restricted_access = Arrays.asList(Role.ADMIN);
        asset.funding = "Funding secured";
        asset.file_formats = Arrays.asList(FileFormat.RAW);
        asset.payload_type = "Conventional";
        asset.digitiser = "Diane Digitiser";
        assetService.updateAsset(asset);
        Optional<Asset> updateAsset = assetService.getAsset("updateAsset");
        assertThat(updateAsset.isPresent()).isTrue();
        Asset result = updateAsset.get();
        assertThat(result.tags.isEmpty()).isTrue();
        // The pipeline and workstation fields on asset is the ones used to create the assets.
        // The ones set on the updated asset is used on the update event and is not displayed on the asset
        assertThat(result.pipeline).isEqualTo("i1_p1");
        assertThat(result.workstation).isEqualTo("i1_w1");
        assertThat(result.status).isEqualTo(AssetStatus.ISSUE_WITH_METADATA);
        assertThat(result.subject).isEqualTo("new sub");
        assertThat(result.restricted_access.get(0)).isEqualTo(Role.ADMIN);
        assertThat(result.funding).isEqualTo("Funding secured");
        assertThat(result.file_formats.size()).isEqualTo(1);
        assertThat(result.file_formats.get(0)).isEqualTo(FileFormat.RAW);
        assertThat(result.payload_type).isEqualTo("Conventional");
        //Digitiser is the original creator of the asset. The name of the new digitiser appears on the update event in the graph
        assertThat(result.digitiser).isEqualTo("Karl-Børge");
        assertThat(result.specimens).hasSize(1);

        //Verify that the asset with barcode creatAsset-sp-1 is removed and the remaining is updated
         Specimen specimen = result.specimens.get(0);
        assertThat(specimen.preparation_type()).isEqualTo("slide");
        assertThat(specimen.specimen_pid()).isEqualTo("spid2");

    }

    @Test
    void lockUnlockAsset() {
        Asset asset = getTestAsset("lockUnlockAsset");
//        asset.specimen_barcodes = Arrays.asList("createAsset-sp-1", "createAsset-sp-2");
        asset.pipeline = "i1_p2";
        asset.workstation = "i1_w2";
        asset.institution = "institution_1";
        asset.collection = "i1_c2";
        asset.asset_pid = "pid-lockUnlock";
        asset.asset_locked = true;
        asset.status = AssetStatus.BEING_PROCESSED;
        assetService.persistAsset(asset, user, 11);
        Optional<Asset> lockedAssetOpt = assetService.getAsset("lockUnlockAsset");
        Asset lockedAsset = lockedAssetOpt.get();
        assertThat(lockedAsset.asset_locked).isTrue();
        assetService.unlockAsset(asset.asset_guid);
        Optional<Asset> unlockedAssetOpt = assetService.getAsset("lockUnlockAsset");
        Asset unlockedAsset = unlockedAssetOpt.get();
        assertThat(unlockedAsset.asset_locked).isFalse();
    }

    @Test
    void auditAsset() {
        Asset asset = getTestAsset("auditAsset");
//        asset.specimen_barcodes = Arrays.asList("auditAsset-sp-1");
        asset.pipeline = "i2_p1";
        asset.workstation = "i2_w1";
        asset.institution = "institution_2";
        asset.collection = "i1_c2";
        asset.asset_pid = "pid-auditAsset";
        asset.asset_locked = false;
        asset.status = AssetStatus.BEING_PROCESSED;
        assetService.persistAsset(asset, user,11);
        DasscoIllegalActionException illegalActionException1 = assertThrows(DasscoIllegalActionException.class, () -> assetService.auditAsset(new Audit("Karl-Børge"), asset.asset_guid));
        assertThat(illegalActionException1).hasMessageThat().isEqualTo("Asset must be complete before auditing");
//        assetService.completeAsset(asset.asset_guid);
        assetService.completeAsset(new AssetUpdateRequest(null, new MinimalAsset("auditAsset", null, null, null),"i2_w1", "i2_p1", "bob"));
        DasscoIllegalActionException illegalActionException2 = assertThrows(DasscoIllegalActionException.class, () -> assetService.auditAsset(new Audit("Karl-Børge"), asset.asset_guid));
        assertThat(illegalActionException2).hasMessageThat().isEqualTo("Audit cannot be performed by the user who digitized the asset");
    }

    @Test
    void testBulkUpdate(){
        // TODO: Make the test a little bit more interesting:
        // Create three different assets
        Asset firstAsset = getTestAsset("bulk-asset-1");
        Asset secondAsset = getTestAsset("bulk-asset-2");
        Asset thirdAsset = getTestAsset("bulk-asset-3");
        firstAsset.institution = "institution_2";
        secondAsset.institution = "institution_2";
        thirdAsset.institution = "institution_2";
        firstAsset.pipeline = "i2_p1";
        secondAsset.pipeline = "i2_p1";
        thirdAsset.pipeline = "i2_p1";
        firstAsset.workstation = "i2_w1";
        secondAsset.workstation = "i2_w1";
        thirdAsset.workstation = "i2_w1";
        firstAsset.collection = "i2_c1";
        secondAsset.collection = "i2_c1";
        thirdAsset.collection = "i2_c1";
        firstAsset.tags.put("First Tag First Asset", "first value first asset");
        secondAsset.tags.put("First Tag Second Asset", "first value second asset");
        secondAsset.funding = "1234";
        thirdAsset.funding = "7894";
        firstAsset.asset_pid = "pid-test-asset-1";
        secondAsset.asset_pid = "pid-test-asset-2";
        thirdAsset.asset_pid = "pid-test-asset-3";
        firstAsset.status = AssetStatus.BEING_PROCESSED;
        secondAsset.status = AssetStatus.BEING_PROCESSED;
        thirdAsset.status = AssetStatus.BEING_PROCESSED;
        assetService.persistAsset(firstAsset, user, 1);
        assetService.persistAsset(secondAsset, user, 1);
        assetService.persistAsset(thirdAsset, user, 1);

        Asset updatedAsset = getTestAsset("updatedAsset");
        updatedAsset.institution = "institution_2";
        updatedAsset.pipeline = "i2_p1";
        updatedAsset.workstation = "i2_w1";
        updatedAsset.collection = "i2_c1";
        updatedAsset.status = AssetStatus.BEING_PROCESSED;
        updatedAsset.updateUser = "test-user";
        updatedAsset.funding = "a lot of funding for testing";
        updatedAsset.tags.put("tag number one for testing", "value number one for testing");

        List<String> assetList = new ArrayList<String>();
        assetList.add("bulk-asset-1");
        assetList.add("bulk-asset-2");
        assetList.add("bulk-asset-3");
        // Update assets with the new asset information:
        assetService.bulkUpdate(assetList, updatedAsset);

        Optional<Asset> optionalUpdatedFirstAsset = assetService.getAsset("bulk-asset-1");
        Optional<Asset> optionalUpdatedSecondAsset = assetService.getAsset("bulk-asset-2");
        Optional<Asset> optionalUpdatedThirdAsset = assetService.getAsset("bulk-asset-3");

        assertThat(optionalUpdatedFirstAsset.isPresent()).isTrue();
        assertThat(optionalUpdatedSecondAsset.isPresent()).isTrue();
        assertThat(optionalUpdatedThirdAsset.isPresent()).isTrue();

        Asset updatedFirstAsset = optionalUpdatedFirstAsset.get();
        Asset updatedSecondAsset = optionalUpdatedSecondAsset.get();
        Asset updatedThirdAsset = optionalUpdatedThirdAsset.get();

        // Finally, the assertions:
        assertThat(updatedFirstAsset.funding).isEqualTo("a lot of funding for testing");
        assertThat(updatedSecondAsset.funding).isEqualTo("a lot of funding for testing");
        assertThat(updatedThirdAsset.funding).isEqualTo("a lot of funding for testing");
    }

    public Asset getTestAsset(String guid) {
        Asset asset = new Asset();
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