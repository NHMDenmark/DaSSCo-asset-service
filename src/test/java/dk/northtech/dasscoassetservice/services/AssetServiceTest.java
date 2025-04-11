package dk.northtech.dasscoassetservice.services;

import com.google.gson.Gson;
import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.*;

class AssetServiceTest extends AbstractIntegrationTest {

    //    @Inject
//    AssetService assetService;
//
    @BeforeEach
    void init() {
        if (user == null) {

            user = userService.ensureExists(new User("Teztuzer"));
        }
    }

    User user = null;


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
        assetService.persistAsset(asset, user, 10);
        List<Event> events = assetService.getEvents("assetEvents", user);
        assertThat(events.size()).isAtLeast(1);
        assertThat(events.get(0).event).isEqualTo(DasscoEvent.CREATE_ASSET_METADATA);
    }


    @Test
    void testValidateAssetFieldsNoAssetGuid() {
        Asset asset = new Asset();
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.validateAssetFields(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("asset_guid cannot be null");
    }

    @Test
    void testValidateAssetFieldsNoAssetPid() {
        Asset asset = new Asset();
        asset.asset_guid = "validateAssetFieldsNoAssetPid";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.validateAssetFields(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("asset_pid cannot be null");
    }

    @Test
    void testValidateAssetFieldsNoStatus() {
        Asset asset = new Asset();
        asset.asset_guid = "validateAssetFieldsNoStatus";
        asset.asset_pid = "pid-validateAssetFieldsNoStatus";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.validateAssetFields(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Status cannot be null");
    }

    @Test
    void testValidateAssetNoInstitution() {
        Asset asset = new Asset();
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.validateNewAssetAndSetIds(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Institution cannot be null");
    }

    @Test
    void testValidateAssetNoInstitutionDoesntExist() {
        Asset asset = new Asset();
        asset.institution = "doesnt exist";
        asset.collection = "collection_1";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.validateNewAssetAndSetIds(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Institution doesnt exist");
    }

    @Test
    void testValidateAssetCollectionDoesntExist() {
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.digitiser = "Bazviola";
        asset.collection = "doesnt ecksist";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.valiedateAndSetCollectionId(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Collection doesnt exist");
    }

    @Test
    void testValidateAssetNoCollection() {
        Asset asset = new Asset();
        asset.institution = "institution_2";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.validateNewAssetAndSetIds(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Collection cannot be null");
    }

    @Test
    void testValidateAssetNoPipeline() {
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.pipeline = "i50_p1250";
        asset.asset_guid = "testValidateAssetNoPipeline";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.validateAsset(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Pipeline doesnt exist in this institution");

    }

    @Test
    void testValidateAssetOwnParent() {
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.pipeline = "i2_p1";
        asset.asset_guid = "assetOwnParent";
        asset.parent_guids = Set.of("assetOwnParent");
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.validateAsset(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset cannot be its own parent");
    }

    @Test
    void testValidateAssetNoWorkstation() {
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.pipeline = "i2_p1";
        asset.asset_guid = "assetNoWorkstation";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.validateAsset(asset));
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
        DasscoIllegalActionException dasscoIllegalActionException = assertThrows(DasscoIllegalActionException.class, () -> assetService.validateAsset(asset));
        assertThat(dasscoIllegalActionException).hasMessageThat().isEqualTo("Workstation [OUT_OF_SERVICE] is marked as out of service");
    }

    @Test
    void testPersistAsset() {
        extendableEnumService.persistEnum(ExtendableEnumService.ExtendableEnum.ISSUE_CATEGORY, "Very big issue");
        Asset createAsset = getTestAsset("createAsset");
        createAsset.updating_pipeline = "i1_p1";
        createAsset.complete_digitiser_list = Arrays.asList("Bazviola", "Karl-Børge");
        createAsset.pipeline = "i1_p1";
        createAsset.workstation = "i1_w1";
        createAsset.legal = new Legality("Copy-rite", "loicense", "kredit");
        createAsset.tags.put("Tag1", "value1");
        createAsset.tags.put("Tag2", "value2");
        createAsset.institution = "institution_1";
        createAsset.specimens = Arrays.asList(new Specimen(createAsset.institution, "i1_c1", "creatAsset-sp-1", "spid1", "slide", null, null), new Specimen(createAsset.institution, "i1_c1", "creatAsset-sp-2", "spid2", "pinning", null, null));
        createAsset.collection = "i1_c1";
        createAsset.asset_pid = "pid-createAsset";
        createAsset.status = "BEING_PROCESSED";
//        createAsset.issues = Arrays.asList(new Issue("It aint working"), new Issue("Substance abuse"));
        createAsset.funding = Arrays.asList("Hundredetusindvis af dollars", "Jeg er stadig i chok");
        createAsset.file_formats = Arrays.asList("PNG", "PDF");
        createAsset.issues = Arrays.asList(new Issue(createAsset.asset_guid, "Very big issue", "issue_1", Instant.now(), "500 ok", "This is an issue", "Notes", false));

        assetService.persistAsset(createAsset, user, 10);

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
        assertThat(result.parent_guids).isEmpty();
        assertThat(result.metadata_version).isEqualTo("1.0.0");
        assertThat(result.metadata_source).isEqualTo("I made it up");
        assertThat(result.camera_setting_control).isEqualTo("Mom get the camera!");
        assertThat(result.date_asset_taken).isNotNull();
        assertThat(result.date_metadata_ingested).isNotNull();
        assertThat(result.date_asset_finalised).isNotNull();
        assertThat(result.push_to_specify).isTrue();
        assertThat(result.make_public).isTrue();
        assertThat(result.payload_type).isEqualTo("nuclear");
        assertThat(result.digitiser).isEqualTo("Karl-Børge");
        assertThat(result.complete_digitiser_list).hasSize(2);
        assertThat(result.complete_digitiser_list).contains("Karl-Børge");
        assertThat(result.complete_digitiser_list).contains("Bazviola");
        assertThat(result.funding).hasSize(2);
        assertThat(result.funding).contains("Hundredetusindvis af dollars");
        assertThat(result.funding).contains("Jeg er stadig i chok");
//        assertThat(result.issues).hasSize(2);
//        assertThat(result.issues).contains(new Issue("Substance abuse"));
//        assertThat(result.issues).contains(new Issue("It aint working"));
//        assertThat(result.file_formats).hasSize(2);
//        assertThat(result.file_formats).contains("PDF");
//        assertThat(result.file_formats).contains("PNG");
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
        assertThat(result.legal).isNotNull();
        assertThat(result.legal.legality_id()).isAtLeast(1);
        assertThat(result.legal.copyright()).isEqualTo("Copy-rite");
        assertThat(result.legal.license()).isEqualTo("loicense");
        assertThat(result.legal.credit()).isEqualTo("kredit");
        Issue issue = result.issues.get(0);
        assertThat(issue.issue_id()).isNotNull();
        assertThat(issue.category()).isEqualTo("Very big issue");
        assertThat(issue.solved()).isFalse();
        assertThat(issue.notes()).isEqualTo("Notes");
        assertThat(issue.description()).isEqualTo("This is an issue");
        assertThat(issue.timestamp()).isNotNull();
        assertThat(issue.name()).isEqualTo("issue_1");
        assertThat(issue.status()).isEqualTo("500 ok");

//        while(true) {
//            try {
//                Thread.sleep(10000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
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
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.persistAsset(asset, user, 0));
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
        asset.parent_guids = Set.of("does_not_exist");
        asset.digitiser = "Bob";
        asset.status = "BEING_PROCESSED";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.persistAsset(asset, user, 10));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Parent doesnt exist");
    }

    @Test
    void testDeleteAssetAssetDoesntExist() {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.deleteAsset("non-existent-asset", user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset doesnt exist!");
    }

    @Test
    void testDeleteAssetAssetIsLocked() {
        Asset asset = getTestAsset("deleteAssetLocked");
        asset.pipeline = "i2_p1";
        asset.workstation = "i2_w1";
        asset.tags.put("Tag1", "value1");
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.asset_pid = "pid-deleteAssetLocked";
        asset.status = "BEING_PROCESSED";
        asset.asset_locked = true;
        assetService.persistAsset(asset, user, 10);
        DasscoIllegalActionException dasscoIllegalActionException = assertThrows(DasscoIllegalActionException.class, () -> assetService.deleteAsset("deleteAssetLocked", user));
        assertThat(dasscoIllegalActionException).hasMessageThat().isEqualTo("Asset is locked");
    }

    @Test
    void testDeleteAssetIsAlreadyDeleted() {
        Asset asset = getTestAsset("deleteAssetIsAlreadyDeleted");
        asset.institution = "institution_2";
        asset.workstation = "i2_w1";
        asset.pipeline = "i2_p1";
        asset.collection = "i2_c1";
        asset.asset_pid = "pid-deleteAssetIsAlreadyDeleted";
        asset.status = "BEING_PROCESSED";
        assetService.persistAsset(asset, user, 1);
        assertThat(assetService.deleteAsset("deleteAssetIsAlreadyDeleted", user)).isTrue();
        Optional<Asset> optionalAsset = assetService.getAsset("deleteAssetIsAlreadyDeleted");
        assertThat(optionalAsset.isPresent()).isTrue();
        Asset deletedAsset = optionalAsset.get();
        assertThat(deletedAsset.events.get(0).event).isEqualTo(DasscoEvent.DELETE_ASSET_METADATA);
        assertThat(deletedAsset.date_asset_deleted).isNotNull();
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.deleteAsset("deleteAssetIsAlreadyDeleted", user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset is already deleted");
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
        assetService.persistAsset(asset, user, 10);
        Asset asset2 = new Asset();
        asset2.institution = "institution_2";
        asset2.workstation = "i2_w1";
        asset2.pipeline = "i2_p1";
        asset2.collection = "i2_c1";
        asset2.asset_guid = "testParentChildRelationIsNotDeletedWhenUpdatingParent_c";
        asset2.asset_pid = "pid_testParentChildRelationIsNotDeletedWhenUpdatingParent_c";
        asset2.parent_guids = Set.of(asset.asset_guid);
        asset2.digitiser = "Bob";
        asset2.status = "BEING_PROCESSED";
        assetService.persistAsset(asset2, user, 10);
        Asset parent = assetService.getAsset(asset.asset_guid).get();
        parent.updateUser = "Bob";
        parent.funding = Arrays.asList("Hundredetusindvis af dollar, jeg er stadig i chok");
        assetService.updateAsset(parent, user);
        Asset child = assetService.getAsset(asset2.asset_guid).get();
        assertWithMessage("Parent should not be deleted").that(child.parent_guids).isNotEmpty();
    }


    @Test
    void testPersistAssetCannotSaveSameAssetTwice() {
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.workstation = "i2_w1";
        asset.pipeline = "i2_p1";
        asset.updating_pipeline = "i2p1";
        asset.collection = "i2_c1";
        asset.asset_guid = "persistAssetCannotSaveSameAssetTwice";
        asset.asset_pid = "pid-persistAssetCannotSaveSameAssetTwice";
        asset.digitiser = "Karl-Børge";
        asset.status = "BEING_PROCESSED";
        assetService.persistAsset(asset, user, 1);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.persistAsset(asset, user, 1));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset persistAssetCannotSaveSameAssetTwice already exists");
    }


    public Asset getTestAsset(String guid) {
        Asset asset = new Asset();
        asset.asset_locked = false;
        asset.status = "BEING_PROCESSED";
        asset.digitiser = "Karl-Børge";
        asset.asset_guid = guid;
        asset.asset_pid = guid + "_pid";
        asset.funding = Arrays.asList("Hundredetusindvis af dollars");
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
        createAsset.specimens = Arrays.asList(new Specimen(createAsset.institution, "i1_c1", "creatAsset-sp-1", "spid1", "slide", null, null));
        createAsset.pipeline = "i1_p1";
        createAsset.workstation = "i1_w1";
        createAsset.tags.put("Tag1", "value1");
        createAsset.tags.put("Tag2", "value2");
        createAsset.institution = "institution_1";
        createAsset.collection = "i1_c1";
        createAsset.asset_pid = "pid-createAsset";
        createAsset.status = "BEING_PROCESSED";
        assetService.persistAsset(createAsset, user, 10);
        Optional<Asset> resultOpt = assetService.getAsset("createAssetUpdateAsset");
        assertThat(resultOpt.isPresent()).isTrue();
        Asset result = resultOpt.get();
        result.updateUser = "Uffe Updater";
        result.payload_type = "conventional";
        assetService.updateAsset(result, user);
        result.payload_type = "nuclear";
        assetService.updateAsset(result, user);
        assetService.completeAsset(new AssetUpdateRequest(new MinimalAsset("createAssetUpdateAsset", null, null, null), "i1_w1", "i1_p1", "bob"), user);
        assetService.auditAsset(user, new Audit("Audrey Auditor"), "createAssetUpdateAsset");
        List<Event> resultEvents = assetService.getEvents(result.asset_guid, user);
        resultEvents.forEach(x -> System.out.println(x));
        assertThat(resultEvents.size()).isEqualTo(5);
        Optional<Asset> resultOpt2 = assetService.getAsset("createAssetUpdateAsset");
        assertThat(resultOpt2.isPresent()).isTrue();
        Asset resultAsset = resultOpt2.get();
        assertThat(resultAsset.payload_type).isEqualTo("nuclear");
        Instant latestUpdate;
        List<Instant> updates = resultEvents.stream()
                .filter(x -> x.event.equals(DasscoEvent.UPDATE_ASSET_METADATA))
                .map(x -> x.timestamp)
                .sorted().toList();
        //The last update event
        assertThat(resultAsset.date_metadata_updated).isEqualTo(updates.get(1));
        assertThat(resultAsset.audited).isTrue();
    }

    @Test
    void testUpdateIssuesCreateOnUpdate() {
        extendableEnumService.persistEnum(ExtendableEnumService.ExtendableEnum.ISSUE_CATEGORY, "test_issue_create");
        var issue_1 = new Issue("testUpdateIssuesCreateOnUpdate", "test_issue_create", "iss_1", Instant.now(), "iss_1_status", "iss_1_description", "iss_1_notest", true);
        var issue_2 = new Issue("testUpdateIssuesCreateOnUpdate", "test_issue_create", "iss_2", Instant.now(), "iss_2_status", "iss_2_description", "iss_2_notest", false);

        Asset createAsset = getTestAsset("testUpdateIssuesCreateOnUpdate");
        createAsset.specimens = Arrays.asList(new Specimen(createAsset.institution, "i1_c1", "creatAsset-sp-1", "spid1", "slide", null, null));
        createAsset.pipeline = "i1_p1";
        createAsset.workstation = "i1_w1";
        createAsset.tags.put("Tag1", "value1");
        createAsset.tags.put("Tag2", "value2");
        createAsset.institution = "institution_1";
        createAsset.collection = "i1_c1";
        createAsset.asset_pid = "pid-createAsset";
        createAsset.status = "BEING_PROCESSED";
        assetService.persistAsset(createAsset, user, 10);
        Optional<Asset> resultOpt = assetService.getAsset("testUpdateIssuesCreateOnUpdate");
        assertThat(resultOpt.isPresent()).isTrue();
        Asset result = resultOpt.get();
        assertThat(result.issues).isEmpty();
        result.issues = Arrays.asList(issue_1, issue_2);
        assetService.updateAsset(result, user);
        Optional<Asset> result2 = assetService.getAsset("testUpdateIssuesCreateOnUpdate");
        Asset asset = result2.get();
        List<Issue> issues = asset.issues;
        assertThat(issues.size()).isEqualTo(2);
        Issue iss_1 = null;
        for(Issue issue:issues) {
            if(issue.name().equals("iss_1")) {
                iss_1 = issue;
//                var issue_1 = new Issue("testUpdateIssuesCreateOnUpdate", "test_issue_create", "iss_1", Instant.now(), "iss_1_status", "iss_1_description", "iss_1_notest", true);
//                var issue_2 = new Issue("testUpdateIssuesCreateOnUpdate", "test_issue_create", "iss_2", Instant.now(), "iss_2_status", "iss_2_description", "iss_2_notest", true);
                assertThat(issue.asset_guid()).isEqualTo(asset.asset_guid);
                assertThat(issue.category()).isEqualTo("test_issue_create");
                assertThat(issue.timestamp()).isNotNull();
                assertThat(issue.status()).isEqualTo("iss_1_status");
                assertThat(issue.description()).isEqualTo("iss_1_description");
                assertThat(issue.notes()).isEqualTo("iss_1_notest");
                assertThat(issue.solved()).isTrue();
                assertThat(issue.issue_id()).isGreaterThan(0);

            } else if (issue.name().equals("iss_2")){
                assertThat(issue.asset_guid()).isEqualTo(asset.asset_guid);
                assertThat(issue.category()).isEqualTo("test_issue_create");
                assertThat(issue.timestamp()).isNotNull();
                assertThat(issue.status()).isEqualTo("iss_2_status");
                assertThat(issue.description()).isEqualTo("iss_2_description");
                assertThat(issue.notes()).isEqualTo("iss_2_notest");
                assertThat(issue.solved()).isFalse();
                assertThat(issue.issue_id()).isGreaterThan(0);

            } else {
                fail("The expected issues were not found");
            }
        }
        asset.issues = List.of(new Issue(issue_1.issue_id(),issue_1.asset_guid(),issue_1.category(),"new_name", Instant.now(), "new_status", "new_desc", "new_notes", false));
        assetService.updateAsset(asset, user);
        Optional<Asset> result3Opt = assetService.getAsset("testUpdateIssuesCreateOnUpdate");
        Asset result3 = result3Opt.get();
        assertThat(result3.issues.size()).isEqualTo(1);
        Issue issue = result3.issues.get(0);
        assertThat(issue.notes()).isEqualTo("new_notes");
        assertThat(issue.name()).isEqualTo("new_name");
        assertThat(issue.description()).isEqualTo("new_desc");

    }



    @Test
    void updateAssetAssetDoesntExist() {
        Asset asset = getTestAsset("updateAssetAssetDoesntExist");
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.updateAsset(asset, user));
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
        createAsset = assetService.persistAsset(createAsset, user, 11);

        createAsset.updateUser = "Uffe Updater";

        createAsset.asset_locked = true;
        assetService.updateAsset(createAsset, user);
        createAsset.asset_locked = false;
        Asset finalCreateAsset = createAsset;
        DasscoIllegalActionException illegalActionException = assertThrows(DasscoIllegalActionException.class, () -> assetService.updateAsset(finalCreateAsset, user));
        assertThat(illegalActionException.getMessage()).isEqualTo("Cannot unlock using updateAsset API, use dedicated API for unlocking");

    }

    @Test
    void updateAsset() {
        Asset asset = getTestAsset("updateAsset");
//        asset.specimen_barcodes = Arrays.asList("createAsset-sp-1", "createAsset-sp-2");
        asset.institution = "institution_1";
        asset.specimens = new ArrayList<>(Arrays.asList(new Specimen(asset.institution, "i1_c1", "creatAsset-sp-1", "spid1", "slide", null, null)
                , new Specimen(asset.institution, "i1_c1", "creatAsset-sp-2", "spid1", "pinning", null, null)));
        asset.pipeline = "i1_p1";
        asset.workstation = "i1_w1";
        asset.tags.put("Tag1", "value1");
        asset.tags.put("Tag2", "value2");
        asset.collection = "i1_c1";
        asset.asset_pid = "pid-updateAsset";
        asset.status = "BEING_PROCESSED";

        assetService.persistAsset(asset, user, 11);

        asset.tags.remove("Tag1");
        asset.tags.remove("Tag2");
        asset.date_asset_finalised = Instant.now();
        asset.date_asset_taken = Instant.now();
        asset.date_metadata_ingested = Instant.now();
        asset.specimens = List.of(new Specimen(asset.institution, asset.collection, "creatAsset-sp-2", "spid2", "slide", null, null));
//        asset.specimens.get()
        asset.workstation = "i1_w2";
        asset.pipeline = "i1_p2";
        asset.status = "ISSUE_WITH_METADATA";
        asset.subject = "new sub";
        asset.restricted_access = Arrays.asList(InternalRole.ADMIN);
        asset.funding = Arrays.asList("420", "Funding secured");
        asset.payload_type = "Conventional";
        asset.digitiser = "Diane Digitiser";
        asset.metadata_version = "One point oh-uh";
        asset.metadata_source = "It came to me in a dream";
        asset.make_public = false;
        asset.push_to_specify = false;
        asset.file_formats = Arrays.asList("PDF", "PNG");
        asset.complete_digitiser_list = Arrays.asList("Karl-Børge", "Viola");
//        asset.issues = Arrays.asList(new Issue("no issues"));
        assetService.updateAsset(asset, user);
        System.out.println("hej1");
        assetService.updateAsset(asset, user);
        System.out.println("hej2");

        Optional<Asset> updateAsset = assetService.getAsset("updateAsset");
        System.out.println("hej3");
        assertThat(updateAsset.isPresent()).isTrue();
        Asset result = updateAsset.get();

        assertThat(result.tags.isEmpty()).isTrue();
        // The pipeline and workstation fields on asset is the ones used to create the assets.
        // The ones set on the updated asset is used on the update event and is not displayed on the asset
        assertThat(result.pipeline).isEqualTo("i1_p1");
        assertThat(result.workstation).isEqualTo("i1_w1");
        assertThat(result.status).isEqualTo("ISSUE_WITH_METADATA");
        assertThat(result.subject).isEqualTo("new sub");


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
        assertThat(result.funding).contains("420");
        assertThat(result.funding).contains("Funding secured");
        assertThat(result.funding).hasSize(2);
//        assertThat(result.issues).hasSize(1);
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
        assetService.persistAsset(asset, user, 1);
        asset.updateUser = "karl-børge";
        //verify that user cant update without write access
        assertThrows(DasscoIllegalActionException.class, () -> assetService.updateAsset(asset, user));
        //verify that user cant update with read access
        assertThrows(DasscoIllegalActionException.class, () -> assetService.updateAsset(asset, new User("karl-børge", new HashSet<>(Arrays.asList("READ_updateAssetNoWritePermission_1")))));
        //verify that it works when user have write access
        assetService.updateAsset(asset, userService.ensureExists(new User("karl-børge", new HashSet<>(Arrays.asList("WRITE_updateAssetNoWritePermission_1")))));
    }


    @Test
    void testCompleteAssetAssetDoesntExist() {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.completeAsset(new AssetUpdateRequest(new MinimalAsset("non-existent-asset", null, null, null), "i1_w1", "i1_p1", "bob"), user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset doesnt exist!");
    }

    @Test
    void testCompleteUpload() {
        Asset asset = getTestAsset("assetUploadComplete");
        asset.pipeline = "i2_p1";
        asset.updating_pipeline = "i2_p1";
        asset.workstation = "i2_w1";
        asset.tags.put("Tag1", "value1");
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.asset_pid = "pid-assetUploadComplete";
        asset.status = "BEING_PROCESSED";
        assetService.persistAsset(asset, user, 1);
        assetService.completeUpload(new AssetUpdateRequest(new MinimalAsset("assetUploadComplete", null, null, null), "i2_w1", "i2_p1", "bob"), user);
        Optional<Asset> optAsset = assetService.getAsset("assetUploadComplete");
        assertThat(optAsset.isPresent()).isTrue();
        assertThat(optAsset.get().internal_status.toString()).isEqualTo("ASSET_RECEIVED");
    }

    @Test
    void testCompleteUploadAssetIsNull() {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.completeUpload(new AssetUpdateRequest(null, "i2_w1", "i2_p1", "bob"), user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset cannot be null");
    }

    @Test
    void testCompleteUploadAssetDoesntExist() {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.completeUpload(new AssetUpdateRequest(new MinimalAsset("non-existent-asset", null, null, null), "i2_w1", "i2_p1", "bob"), user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset doesnt exist!");
    }

    @Test
    void testCompleteUploadAssetIsLocked() {
        Asset asset = getTestAsset("completeUploadAssetIsLocked");
        asset.pipeline = "i2_p1";
        asset.workstation = "i2_w1";
        asset.tags.put("Tag1", "value1");
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.asset_pid = "pid-completeUploadAssetIsLocked";
        asset.status = "BEING_PROCESSED";
        asset.asset_locked = true;
        assetService.persistAsset(asset, user, 1);
        DasscoIllegalActionException dasscoIllegalActionException = assertThrows(DasscoIllegalActionException.class, () -> assetService.completeUpload(new AssetUpdateRequest(new MinimalAsset("completeUploadAssetIsLocked", null, null, null), "i2_w1", "i2_p1", "bob"), user));
        assertThat(dasscoIllegalActionException).hasMessageThat().isEqualTo("Asset is locked");
    }

    @Test
    void testCompleteAsset() {
        Asset asset = getTestAsset("assetComplete");
        asset.pipeline = "i2_p1";
        asset.workstation = "i2_w1";
        asset.tags.put("Tag1", "value1");
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.asset_pid = "pid-assetComplete";
        asset.status = "BEING_PROCESSED";
        assetService.persistAsset(asset, user, 1);
        Optional<Asset> optAsset = assetService.getAsset("assetComplete");
        assertThat(optAsset.isPresent()).isTrue();
        assertThat(optAsset.get().internal_status.toString()).isEqualTo("METADATA_RECEIVED");
        assertThat(assetService.completeAsset(new AssetUpdateRequest(new MinimalAsset("assetComplete", null, null, null), "i2_w1", "i2_p1", "bob"), user)).isTrue();
        Optional<Asset> optCompletedAsset = assetService.getAsset("assetComplete");
        assertThat(optCompletedAsset.isPresent()).isTrue();
        assertThat(optCompletedAsset.get().internal_status.toString()).isEqualTo("COMPLETED");
    }

    @Test
    void testSetAssetStatus() {
        Asset asset = getTestAsset("setAssetStatus");
        asset.pipeline = "i2_p1";
        asset.workstation = "i2_w1";
        asset.tags.put("Tag1", "value1");
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.asset_pid = "pid-setAssetStatus";
        asset.status = "BEING_PROCESSED";
        assetService.persistAsset(asset, user, 1);
        assetService.setAssetStatus("setAssetStatus", "ERDA_ERROR", "");
        Optional<Asset> optAsset = assetService.getAsset("setAssetStatus");
        assertThat(optAsset.isPresent()).isTrue();
        assertThat(optAsset.get().internal_status.toString()).isEqualTo("ERDA_ERROR");
    }

    @Test
    void testSetAssetStatusInvalidStatus() {
        Asset asset = getTestAsset("setAssetStatusInvalidStatus");
        asset.pipeline = "i2_p1";
        asset.workstation = "i2_w1";
        asset.tags.put("Tag1", "value1");
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.asset_pid = "pid-setAssetStatusInvalidStatus";
        asset.status = "BEING_PROCESSED";
        assetService.persistAsset(asset, user, 1);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.setAssetStatus("setAssetStatusInvalidStatus", "INVALID_STATUS", ""));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Invalid status: INVALID_STATUS");
        Optional<Asset> optAsset = assetService.getAsset("setAssetStatusInvalidStatus");
        assertThat(optAsset.isPresent()).isTrue();
        assertThat(optAsset.get().internal_status.toString()).isEqualTo("METADATA_RECEIVED");
    }

    @Test
    void testSetAssetStatusUnsupportedStatus() {
        Asset asset = getTestAsset("setAssetStatusUnsupportedStatus");
        asset.pipeline = "i2_p1";
        asset.updating_pipeline = "i2_p1";
        asset.workstation = "i2_w1";
        asset.tags.put("Tag1", "value1");
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.asset_pid = "pid-setAssetStatusUnsupportedStatus";
        asset.status = "BEING_PROCESSED";
        assetService.persistAsset(asset, user, 1);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.setAssetStatus("setAssetStatusUnsupportedStatus", "COMPLETED", ""));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Invalid status: COMPLETED");
        Optional<Asset> optAsset = assetService.getAsset("setAssetStatusUnsupportedStatus");
        assertThat(optAsset.isPresent()).isTrue();
        assertThat(optAsset.get().internal_status.toString()).isEqualTo("METADATA_RECEIVED");
    }

    @Test
    void testSetAssetAssetDoesntExist() {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.setAssetStatus("non-existent-asset", "ERDA_ERROR", ""));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset doesnt exist!");
    }

    @Test
    void testLockUnlockAsset() {
        Asset asset = getTestAsset("lockUnlockAsset");
//        asset.specimen_barcodes = Arrays.asList("createAsset-sp-1", "createAsset-sp-2");
        asset.pipeline = "i1_p2";
        asset.workstation = "i1_w2";
        asset.institution = "institution_1";
        asset.collection = "i1_c2";
        asset.asset_pid = "pid-lockUnlock";
        asset.asset_locked = true;
        asset.status = "BEING_PROCESSED";
        assetService.persistAsset(asset, user, 11);
        Optional<Asset> lockedAssetOpt = assetService.getAsset("lockUnlockAsset");
        assertThat(lockedAssetOpt.isPresent()).isTrue();
        Asset locked = lockedAssetOpt.get();
        assertThat(locked.asset_locked).isTrue();
//        while(true) {
//            try {
//                Thread.sleep(10000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
        assetService.unlockAsset(asset.asset_guid);

        Optional<Asset> unlockedAssetOpt = assetService.getAsset("lockUnlockAsset");
        assertThat(unlockedAssetOpt.isPresent()).isTrue();
        Asset unlockedAsset = unlockedAssetOpt.get();
        assertThat(unlockedAsset.asset_locked).isFalse();
    }

    @Test
    void testUnlockAssetAssetDoesntExist() {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.unlockAsset("non-existent-asset"));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset doesnt exist!");
    }

}