package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssetService2Test extends AbstractIntegrationTest {

    //    @Inject
//    AssetService assetService;
//
    User user = new User("Teztuzer");


    @Test
    void testGetEvents() {
        Asset asset = getTestAsset("assetEvents");
        asset.pipeline = "i2_p1";
        asset.workstation = "i2_w1";
        asset.tags.put("Tag1", "value1");
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.asset_pid = "pid-assetEvents";
        asset.status = "BEING_PROCESSED";
        assetService2.persistAsset(asset, user, 10);
        List<Event> events = assetService2.getEvents("assetEvents", user);
        assertThat(events.size()).isAtLeast(1);
        assertThat(events.get(0).event).isEqualTo(DasscoEvent.CREATE_ASSET_METADATA);
    }


    @Test
    void testValidateAssetFieldsNoAssetGuid() {
        Asset asset = new Asset();
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService2.validateAssetFields(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("asset_guid cannot be null");
    }

    @Test
    void testValidateAssetFieldsNoAssetPid() {
        Asset asset = new Asset();
        asset.asset_guid = "validateAssetFieldsNoAssetPid";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService2.validateAssetFields(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("asset_pid cannot be null");
    }

    @Test
    void testValidateAssetFieldsNoStatus() {
        Asset asset = new Asset();
        asset.asset_guid = "validateAssetFieldsNoStatus";
        asset.asset_pid = "pid-validateAssetFieldsNoStatus";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService2.validateAssetFields(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Status cannot be null");
    }

    @Test
    void testValidateAssetNoInstitution() {
        Asset asset = new Asset();
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService2.validateNewAsset(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Institution cannot be null");
    }

    @Test
    void testValidateAssetNoInstitutionDoesntExist() {
        Asset asset = new Asset();
        asset.institution = "doesnt exist";
        asset.collection = "collection_1";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService2.validateNewAsset(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Institution doesnt exist");
    }

    @Test
    void testValidateAssetCollectionDoesntExist() {
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.digitiser = "Bazviola";
        asset.collection = "doesnt ecksist";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService2.validateNewAsset(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Collection doesnt exist");
    }

    @Test
    void testValidateAssetNoCollection() {
        Asset asset = new Asset();
        asset.institution = "institution_2";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService2.validateNewAsset(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Collection cannot be null");
    }

    @Test
    void testValidateAssetNoPipeline() {
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.pipeline = "i50_p1250";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService2.validateAsset(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Pipeline doesnt exist in this institution");

    }

    @Test
    void testValidateAssetOwnParent() {
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.pipeline = "i2_p1";
        asset.asset_guid = "assetOwnParent";
        asset.parent_guid = "assetOwnParent";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService2.validateAsset(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset cannot be its own parent");
    }

    @Test
    void testValidateAssetNoWorkstation() {
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.pipeline = "i2_p1";
        asset.asset_guid = "assetNoWorkstation";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService2.validateAsset(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Workstation does not exist");
    }

    @Test
    void testValidateAssetWorkstationOutOfService() {
        Asset asset = new Asset();
        asset.institution = "institution_1";
        asset.collection = "i1_c1";
        asset.pipeline = "i1_p1";
        asset.asset_guid = "validateAssetWorkstationOutOfService";
        asset.workstation = "i1_w3";
        DasscoIllegalActionException dasscoIllegalActionException = assertThrows(DasscoIllegalActionException.class, () -> assetService2.validateAsset(asset));
        assertThat(dasscoIllegalActionException).hasMessageThat().isEqualTo("Workstation [OUT_OF_SERVICE] is marked as out of service");
    }

    @Test
    void testPersistAsset() {
        Asset createAsset = getTestAsset("createAsset");
        createAsset.complete_digitiser_list = Arrays.asList("Bazviola", "Karl-Børge");
        createAsset.pipeline = "i1_p1";
        createAsset.workstation = "i1_w1";
        createAsset.tags.put("Tag1", "value1");
        createAsset.tags.put("Tag2", "value2");
        createAsset.institution = "institution_1";
        createAsset.specimens = Arrays.asList(new Specimen(createAsset.institution, "i1_c1", "creatAsset-sp-1", "spid1", "slide"), new Specimen(createAsset.institution, "i1_c1", "creatAsset-sp-2", "spid2", "pinning"));
        createAsset.collection = "i1_c1";
        createAsset.asset_pid = "pid-createAsset";
        createAsset.status = "BEING_PROCESSED";
        createAsset.issues = Arrays.asList(new Issue("It aint working"), new Issue("Substance abuse"));
        createAsset.funding = Arrays.asList(new Funding("Hundredetusindvis af dollars"),new Funding("Jeg er stadig i chok"));
        createAsset.file_formats = Arrays.asList("PNG", "PDF");
        assetService2.persistAsset(createAsset, user, 10);
        Optional<Asset> resultOpt = assetService2.getAsset("createAsset");

//                while(true) {
//            try {
//                Thread.sleep(10000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }

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
        assertThat(result.metadata_version).isEqualTo("1.0.0");
        assertThat(result.metadata_source).isEqualTo("I made it up");
        assertThat(result.camera_setting_control).isEqualTo("Mom get the camera!");
        assertThat(result.date_asset_taken).isNotNull();
        assertThat(result.date_metadata_ingested).isNotNull();
        assertThat(result.date_asset_finalised).isNotNull();
        assertThat(result.push_to_specify).isTrue();
        assertThat(result.make_public).isTrue();
//        assertThat(result.specimen_barcodes).contains("createAsset-sp-1");
//        assertThat(result.specimen_barcodes).contains("createAsset-sp-2");
        assertThat(result.payload_type).isEqualTo("nuclear");
        assertThat(result.digitiser).isEqualTo("Karl-Børge");
        assertThat(result.complete_digitiser_list).hasSize(2);
        assertThat(result.complete_digitiser_list).contains("Karl-Børge");
        assertThat(result.complete_digitiser_list).contains("Bazviola");
//TODO handle lists here
        assertThat(result.funding).hasSize(2);
        assertThat(result.funding).contains(new Funding("Hundredetusindvis af dollars"));
        assertThat(result.funding).contains(new Funding("Jeg er stadig i chok"));
        assertThat(result.issues).hasSize(2);
        assertThat(result.issues).contains(new Issue("Substance abuse"));
        assertThat(result.issues).contains(new Issue("It aint working"));
        assertThat(result.file_formats).hasSize(2);
        assertThat(result.file_formats).contains("PDF");
        assertThat(result.file_formats).contains("PNG");
        //Specimens
        assertThat(result.specimens).hasSize(2);
        Specimen specimen_1 = result.specimens.get(0).barcode().equals("creatAsset-sp-1") ? result.specimens.get(0) : result.specimens.get(1);
        assertThat(specimen_1.barcode()).isEqualTo("creatAsset-sp-1");
        assertThat(specimen_1.specimen_pid()).isEqualTo("spid1");
        assertThat(specimen_1.preparation_type()).isEqualTo("slide");
        Specimen specimen_2 = result.specimens.get(0).barcode().equals("creatAsset-sp-2") ? result.specimens.get(0) : result.specimens.get(1);
        assertThat(specimen_2.barcode()).isEqualTo("creatAsset-sp-2");
        assertThat(specimen_2.specimen_pid()).isEqualTo("spid2");
        assertThat(specimen_2.preparation_type()).isEqualTo("pinning");
    }

    @Test
    void testPersistAssetAllocationCannotBe0() {
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.workstation = "i2_w1";
        asset.pipeline = "i2_p1";
        asset.collection = "i2_c1";
        asset.digitiser = "Dan Digitiser";
        asset.asset_guid = "persistAssetAllocationCannotBe0";
        asset.asset_pid = "pdididid";
        asset.status = "BEING_PROCESSED";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService2.persistAsset(asset, user, 0));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Allocation cannot be 0");
    }

    @Test
    void testPersistAssetParentMustExist() {
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.workstation = "i2_w1";
        asset.pipeline = "i2_p1";
        asset.collection = "i2_c1";
        asset.asset_guid = "testPersistAssetParentMustExist";
        asset.asset_pid = "pid_testPersistAssetParentMustExist";
        asset.parent_guid = "does_not_exist";
        asset.digitiser = "Bob";
        asset.status = "BEING_PROCESSED";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService2.persistAsset(asset, user, 10));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Parent doesnt exist");
    }

    @Test
    void testParentChildRelationIsNotDeletedWhenUpdatingParent() {
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.workstation = "i2_w1";
        asset.pipeline = "i2_p1";
        asset.collection = "i2_c1";
        asset.asset_guid = "testParentChildRelationIsNotDeletedWhenUpdatingParent_p";
        asset.asset_pid = "pid_testParentChildRelationIsNotDeletedWhenUpdatingParent_p";
//        asset.parent_guid = "does_not_exist";
        asset.digitiser = "Bob";
        asset.status = "BEING_PROCESSED";
        assetService2.persistAsset(asset, user, 10);
        Asset asset2 = new Asset();
        asset2.institution = "institution_2";
        asset2.workstation = "i2_w1";
        asset2.pipeline = "i2_p1";
        asset2.collection = "i2_c1";
        asset2.asset_guid = "testParentChildRelationIsNotDeletedWhenUpdatingParent_c";
        asset2.asset_pid = "pid_testParentChildRelationIsNotDeletedWhenUpdatingParent_c";
        asset2.parent_guid = asset.asset_guid;
        asset2.digitiser = "Bob";
        asset2.status = "BEING_PROCESSED";
        assetService2.persistAsset(asset2, user, 10);
        Asset parent = assetService2.getAsset(asset.asset_guid).get();
        parent.updateUser = "Bob";
        parent.funding = Arrays.asList(new Funding("Hundredetusindvis af dollar, jeg er stadig i chok"));
        assetService2.updateAsset(parent, user);
        Asset child = assetService2.getAsset(asset2.asset_guid).get();
        assertWithMessage("Parent should not be deleted").that(child.parent_guid).isNotEmpty();
    }


    @Test
    void testPersistAssetCannotSaveSameAssetTwice() {
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.workstation = "i2_w1";
        asset.pipeline = "i2_p1";
        asset.collection = "i2_c1";
        asset.asset_guid = "persistAssetCannotSaveSameAssetTwice";
        asset.asset_pid = "pid-persistAssetCannotSaveSameAssetTwice";
        asset.digitiser = "Karl-Børge";
        asset.status = "BEING_PROCESSED";
        assetService2.persistAsset(asset, user, 1);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService2.persistAsset(asset, user, 1));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset persistAssetCannotSaveSameAssetTwice already exists");
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

    @Test
    void testUpdateAsset() {
        Asset createAsset = getTestAsset("createAssetUpdateAsset");
        createAsset.specimens = Arrays.asList(new Specimen(createAsset.institution, "i1_c1", "creatAsset-sp-1", "spid1", "slide"));
        createAsset.pipeline = "i1_p1";
        createAsset.workstation = "i1_w1";
        createAsset.tags.put("Tag1", "value1");
        createAsset.tags.put("Tag2", "value2");
        createAsset.institution = "institution_1";
        createAsset.collection = "i1_c1";
        createAsset.asset_pid = "pid-createAsset";
        createAsset.status = "BEING_PROCESSED";
        assetService2.persistAsset(createAsset, user, 10);
        Optional<Asset> resultOpt = assetService2.getAsset("createAssetUpdateAsset");
        assertThat(resultOpt.isPresent()).isTrue();
        Asset result = resultOpt.get();
        result.updateUser = "Uffe Updater";
        result.payload_type = "conventional";
        assetService2.updateAsset(result, user);
        result.payload_type = "nuclear";
        assetService2.updateAsset(result, user);
        assetService2.completeAsset(new AssetUpdateRequest(new MinimalAsset("createAssetUpdateAsset", null, null, null), "i1_w1", "i1_p1", "bob"));
        assetService2.auditAsset(user, new Audit("Audrey Auditor"), "createAssetUpdateAsset");
        List<Event> resultEvents = assetService2.getEvents(result.asset_guid, user);
        assertThat(resultEvents.size()).isEqualTo(5);
        Optional<Asset> resultOpt2 = assetService2.getAsset("createAssetUpdateAsset");
        assertThat(resultOpt2.isPresent()).isTrue();
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
    void updateAssetAssetDoesntExist() {
        Asset asset = getTestAsset("updateAssetAssetDoesntExist");
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService2.updateAsset(asset, user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset updateAssetAssetDoesntExist does not exist");
    }


    @Test
    void testUpdateAssetdoNotPermitUnlocking() {
        Asset createAsset = getTestAsset("doNotPermitUnlocking");
        createAsset.pipeline = "i1_p1";
        createAsset.workstation = "i1_w1";
        createAsset.tags.put("Tag1", "value1");
        createAsset.tags.put("Tag2", "value2");
        createAsset.institution = "institution_1";
        createAsset.collection = "i1_c1";
        createAsset.asset_pid = "pid-createAsset";
        createAsset.status = "BEING_PROCESSED";
        createAsset = assetService2.persistAsset(createAsset, user, 11);

        createAsset.updateUser = "Uffe Updater";

        createAsset.asset_locked = true;
        assetService2.updateAsset(createAsset, user);
        createAsset.asset_locked = false;
        Asset finalCreateAsset = createAsset;
        DasscoIllegalActionException illegalActionException = assertThrows(DasscoIllegalActionException.class, () -> assetService2.updateAsset(finalCreateAsset, user));
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
        asset.status = "BEING_PROCESSED";
        assetService2.persistAsset(asset, user, 11);
        asset.tags.remove("Tag1");
        asset.tags.remove("Tag2");
        asset.date_asset_finalised = Instant.now();
        asset.date_asset_taken = Instant.now();
        asset.date_metadata_ingested = Instant.now();
        asset.specimens = List.of(new Specimen(asset.institution, asset.collection, "creatAsset-sp-2", "spid2", "slide"));
//        asset.specimens.get()
        asset.workstation = "i1_w2";
        asset.pipeline = "i1_p2";
        asset.status = "ISSUE_WITH_METADATA";
        asset.subject = "new sub";
        asset.restricted_access = Arrays.asList(InternalRole.ADMIN);
        asset.funding = Arrays.asList(new Funding("420"),new Funding("Funding secured"));
        asset.file_formats = Arrays.asList("RAW");
        asset.payload_type = "Conventional";
        asset.digitiser = "Diane Digitiser";
        asset.metadata_version = "One point oh-uh";
        asset.metadata_source = "It came to me in a dream";
        asset.make_public = false;
        asset.push_to_specify = false;
        asset.file_formats = Arrays.asList("PDF", "PNG");
        asset.complete_digitiser_list = Arrays.asList("Karl-Børge", "Viola");
        asset.issues = Arrays.asList(new Issue("no issues"));
        assetService2.updateAsset(asset, user);
        Optional<Asset> updateAsset = assetService2.getAsset("updateAsset");
        assertThat(updateAsset.isPresent()).isTrue();
        Asset result = updateAsset.get();
        //        while(true) {
//            try {
//                Thread.sleep(10000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
        assertThat(result.tags.isEmpty()).isTrue();
        // The pipeline and workstation fields on asset is the ones used to create the assets.
        // The ones set on the updated asset is used on the update event and is not displayed on the asset
        assertThat(result.pipeline).isEqualTo("i1_p1");
        assertThat(result.workstation).isEqualTo("i1_w1");
        assertThat(result.status).isEqualTo("ISSUE_WITH_METADATA");
        assertThat(result.subject).isEqualTo("new sub");

        //TODO handle lists here!
//        assertThat(result.funding.get(0)).isEqualTo("Funding secured");
//        assertThat(result.file_formats.size()).isEqualTo(1);
//        assertThat(result.file_formats.get(0)).isEqualTo("RAW");
        assertThat(result.payload_type).isEqualTo("Conventional");
        assertThat(result.date_asset_finalised).isNotNull();
        assertThat(result.date_asset_taken).isNotNull();
        assertThat(result.date_metadata_ingested).isNotNull();

        assertThat(result.digitiser).isEqualTo("Diane Digitiser");
        assertThat(result.metadata_version).isEqualTo("One point oh-uh");
        assertThat(result.metadata_source).isEqualTo("It came to me in a dream");
        assertThat(result.complete_digitiser_list).contains("Viola");
        assertThat(result.complete_digitiser_list).contains("Karl-Børge");
        assertThat(result.complete_digitiser_list).hasSize(2);
        assertThat(result.file_formats).hasSize(2);
        assertThat(result.file_formats).contains("PDF");
        assertThat(result.file_formats).contains("PNG");
        assertThat(result.funding).contains(new Funding("420"));
        assertThat(result.funding).contains(new Funding("Funding secured"));
        assertThat(result.funding).hasSize(2);
        assertThat(result.issues).hasSize(1);
        assertThat(result.specimens).hasSize(1);
        assertThat(result.make_public).isFalse();
        assertThat(result.push_to_specify).isFalse();
        //Verify that the asset with barcode creatAsset-sp-1 is removed and the remaining is updated
        Specimen specimen = result.specimens.get(0);
        assertThat(specimen.preparation_type()).isEqualTo("slide");
        assertThat(specimen.specimen_pid()).isEqualTo("spid2");

    }

    @Test
    void updateAssetNoWritePermission() {
        collectionService.persistCollection(new Collection("updateAssetNoWritePermission_1", "institution_2", Arrays.asList(new Role("updateAssetNoWritePermission_1"))));
        Asset asset = getTestAsset("udateAssetNoWritePermission");
        asset.asset_pid = "pid-updateAssetNoWritePermission";
        asset.workstation = "i2_w1";
        asset.institution = "institution_2";
        asset.pipeline = "i2_p1";
        asset.collection = "updateAssetNoWritePermission_1";
        asset.status = "BEING_PROCESSED";
        assetService2.persistAsset(asset, user, 1);
        asset.updateUser = "karl-børge";
        //verify that user cant update without write access
        assertThrows(DasscoIllegalActionException.class, () -> assetService2.updateAsset(asset, user));
        //verify that user cant update with read access
        assertThrows(DasscoIllegalActionException.class, () -> assetService2.updateAsset(asset, new User("karl-børge", new HashSet<>(Arrays.asList("READ_updateAssetNoWritePermission_1")))));
        //verify that it works when user have write access
        assetService2.updateAsset(asset, new User("karl-børge", new HashSet<>(Arrays.asList("WRITE_updateAssetNoWritePermission_1"))));
    }


}