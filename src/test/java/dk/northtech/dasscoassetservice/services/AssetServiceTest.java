package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.webapi.domain.HttpAllocationStatus;
import dk.northtech.dasscoassetservice.webapi.domain.HttpInfo;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.bouncycastle.tsp.TSPUtil;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
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
        // Create three different assets
        Asset firstAsset = getBulkUpdateAssetToBeUpdated("bulk-asset-1");
        Asset secondAsset = getBulkUpdateAssetToBeUpdated("bulk-asset-2");

        assetService.persistAsset(firstAsset, user, 1);
        assetService.persistAsset(secondAsset, user, 1);

        Asset updatedAsset = getBulkUpdateAsset();

        // Create list of assets to be updated:
        List<String> assetList = new ArrayList<String>();
        assetList.add("bulk-asset-1");
        assetList.add("bulk-asset-2");
        // Update assets with the new asset information:
        assetService.bulkUpdate(assetList, updatedAsset);

        Optional<Asset> optionalUpdatedFirstAsset = assetService.getAsset("bulk-asset-1");
        Optional<Asset> optionalUpdatedSecondAsset = assetService.getAsset("bulk-asset-2");

        assertThat(optionalUpdatedFirstAsset.isPresent()).isTrue();
        assertThat(optionalUpdatedSecondAsset.isPresent()).isTrue();

        Asset updatedFirstAsset = optionalUpdatedFirstAsset.get();
        Asset updatedSecondAsset = optionalUpdatedSecondAsset.get();

        // Finally, the assertions:

        assertThat(updatedFirstAsset.status).isEqualTo(AssetStatus.BEING_PROCESSED);
        assertThat(updatedSecondAsset.status).isEqualTo(AssetStatus.BEING_PROCESSED);
        /*
        assertThat(updatedFirstAsset.specimens.size()).isEqualTo(1);
        assertThat(updatedFirstAsset.specimens.get(0).barcode()).isEqualTo("BULK_UPDATE_TEST");
        assertThat(updatedSecondAsset.specimens.size()).isEqualTo(1);
        assertThat(updatedSecondAsset.specimens.get(0).barcode()).isEqualTo("BULK_UPDATE_TEST");

         */
        assertThat(updatedFirstAsset.funding).isEqualTo("Hundredetusindvis af dollars");
        assertThat(updatedSecondAsset.funding).isEqualTo("Hundredetusindvis af dollars");
        assertThat(updatedFirstAsset.subject).isEqualTo("Folder");
        assertThat(updatedSecondAsset.subject).isEqualTo("Folder");
        assertThat(updatedFirstAsset.payload_type).isEqualTo("nuclear");
        assertThat(updatedSecondAsset.payload_type).isEqualTo("nuclear");
        assertThat(updatedFirstAsset.file_formats.size()).isEqualTo(1);
        assertThat(updatedFirstAsset.file_formats.get(0)).isEqualTo(FileFormat.JPEG);
        assertThat(updatedSecondAsset.file_formats.size()).isEqualTo(1);
        assertThat(updatedSecondAsset.file_formats.get(0)).isEqualTo(FileFormat.JPEG);
        assertThat(updatedFirstAsset.asset_locked).isTrue();
        assertThat(updatedSecondAsset.asset_locked).isTrue();
        assertThat(updatedFirstAsset.restricted_access.size()).isEqualTo(1);
        assertThat(updatedFirstAsset.restricted_access.get(0)).isEqualTo(Role.DEVELOPER);
        assertThat(updatedSecondAsset.restricted_access.size()).isEqualTo(1);
        assertThat(updatedSecondAsset.restricted_access.get(0)).isEqualTo(Role.DEVELOPER);
        assertThat(updatedFirstAsset.tags.size()).isEqualTo(1);
        assertThat(updatedSecondAsset.tags.size()).isEqualTo(1);
        assertThat(updatedFirstAsset.date_asset_finalised.truncatedTo(ChronoUnit.MILLIS)).isEqualTo(updatedAsset.date_asset_finalised.truncatedTo(ChronoUnit.MILLIS));
        assertThat(updatedSecondAsset.date_asset_finalised.truncatedTo(ChronoUnit.MILLIS)).isEqualTo(updatedAsset.date_asset_finalised.truncatedTo(ChronoUnit.MILLIS));
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
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.bulkUpdate(assetList, updatedAsset));
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

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.bulkUpdate(listOfAssets, updatedAsset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("asset_parent does not exist!");

        Optional<Asset> optNotUpdated = assetService.getAsset("bulk-update-no-parent-2");
        assertThat(optNotUpdated.isPresent()).isTrue();
        Asset notUpdated = optNotUpdated.get();
        // Using funding or subject or payload_type is the easiest way to check if an asset was updated or not.
        assertThat(notUpdated.funding).isEqualTo("funding has depleted");
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

        DasscoIllegalActionException dasscoIllegalActionException = assertThrows(DasscoIllegalActionException.class, () -> assetService.bulkUpdate(assetList, updatedAsset));
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
        /*
        Asset asset = new Asset();
        List<String> assetList = new ArrayList<>();
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.bulkUpdate(assetList, asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Update user must be provided!");

         */
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

    public Asset getBulkUpdateAssetToBeUpdated(String guid){
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.pipeline = "i2_p1";
        asset.workstation = "i2_w1";
        asset.collection = "i2_c1";
        asset.asset_pid = "pid-" + guid;
        asset.asset_guid = guid;
        asset.status = AssetStatus.WORKING_COPY;
        asset.funding = "funding has depleted";
        asset.subject = "subject-non-edited";
        asset.payload_type = "payload-not-edited";
        asset.file_formats.add(FileFormat.CR3);
        asset.file_formats.add(FileFormat.RAF);
        asset.restricted_access.add(Role.USER);
        asset.restricted_access.add(Role.ADMIN);
        asset.tags.put("Tag 1", "Value 1");
        asset.tags.put("Tag 2", "Value 2");
        asset.date_asset_finalised = Instant.now().minus(1, ChronoUnit.DAYS);
        asset.digitiser = "test-user";
        asset.updateUser = "update-user";

        Specimen firstSpecimen = new Specimen("barcode-specimen-1", "pid-specimen-1", "image");
        Specimen secondSpecimen = new Specimen("barcode-specimen-2", "pid-specimen-2", "image");
        List<Specimen> specimenList = Arrays.asList(firstSpecimen, secondSpecimen);

        asset.specimens = specimenList;

        return asset;
    }

    public Asset getBulkUpdateAsset(){
        Asset updatedAsset = getTestAsset("updatedAsset");
        updatedAsset.institution = "institution_2";
        updatedAsset.pipeline = "i2_p1";
        updatedAsset.workstation = "i2_w1";
        updatedAsset.collection = "i2_c1";
        updatedAsset.status = AssetStatus.BEING_PROCESSED;
        Specimen specimen = new Specimen("BULK_UPDATE_TEST", "pid-BULK_UPDATE_TEST", "text");
        List<Specimen> specimenList = new ArrayList<>();
        specimenList.add(specimen);
        updatedAsset.specimens = specimenList;
        updatedAsset.asset_locked = true;
        updatedAsset.restricted_access.add(Role.DEVELOPER);
        updatedAsset.tags.put("Tag 3", "Value 3");
        updatedAsset.date_asset_finalised = Instant.now();
        return updatedAsset;
    }
}