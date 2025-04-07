package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

class AssetServiceTestX extends AbstractIntegrationTest {

//    @Inject
//    AssetService assetService;
//
    User user = new User("Teztuzer");

    @Test
    void testAuditAsset() {
        Asset asset = getTestAsset("auditAsset");
//        asset.specimen_barcodes = Arrays.asList("auditAsset-sp-1");
        asset.pipeline = "i2_p1";
        asset.workstation = "i2_w1";
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.asset_pid = "pid-auditAsset";
        asset.asset_locked = false;
        asset.status = "BEING_PROCESSED";
        assetService.persistAsset(asset, user,11);
//        assetService.completeAsset(asset.asset_guid);
        Optional<Asset> optAsset = assetService.getAsset("auditAsset");
        assertThat(optAsset.isPresent()).isTrue();
        Asset notAudited = optAsset.get();
        assertThat(notAudited.audited).isFalse();
        assetService.completeAsset(new AssetUpdateRequest( new MinimalAsset("auditAsset", null, null, null), "i2_w1", "i2_p1", "bob"),user);
        assetService.auditAsset(user, new Audit("Not-Karl-Børge"), asset.asset_guid);
        Optional<Asset> optionalAsset = assetService.getAsset("auditAsset");
        assertThat(optionalAsset.isPresent()).isTrue();
        Asset exists = optionalAsset.get();
        assertThat(exists.asset_guid).isEqualTo("auditAsset");
        assertThat(exists.asset_pid).isEqualTo("pid-auditAsset");
        assertThat(exists.audited).isTrue();
    }

    @Test
    void testAuditAssetMustHaveUser(){
        Asset asset = getTestAsset("auditAssetNoUser");
        asset.pipeline = "i2_p1";
        asset.workstation = "i2_w1";
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.asset_pid = "pid-auditAssetNoUser";
        asset.asset_locked = false;
        asset.status = "BEING_PROCESSED";
        assetService.persistAsset(asset, user, 10);
        assetService.completeAsset(new AssetUpdateRequest( new MinimalAsset("auditAssetNoUser", null, null, null), "i2_w1", "i2_p1", "bob"),user);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.auditAsset(user,new Audit(""), asset.asset_guid));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Audit must have a user!");
    }

    @Test
    void testAuditAssetAssetMustExist(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.auditAsset(user,new Audit("Karl-Børge"), "non-existent-asset"));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset doesnt exist!");
}

    @Test
    void testAuditAssetHasToBeComplete(){
        Asset asset = getTestAsset("auditAssetMustBeComplete");
        asset.pipeline = "i2_p1";
        asset.workstation = "i2_w1";
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.asset_pid = "pid-auditAssetMustBeComplete";
        asset.asset_locked = false;
        asset.status = "BEING_PROCESSED";
        assetService.persistAsset(asset, user, 10);
        DasscoIllegalActionException illegalActionException1 = assertThrows(DasscoIllegalActionException.class, () -> assetService.auditAsset(user,new Audit("Karl-Børge"), asset.asset_guid));
        assertThat(illegalActionException1).hasMessageThat().isEqualTo("Asset must be complete before auditing");
    }

    @Test
    void testAuditAssetCannotBeAuditedByUserWhoDigitizedIt(){
        Asset asset = getTestAsset("auditAssetCannotBeAuditedBySameUserWhoDigitizedIt");
        asset.pipeline = "i2_p1";
        asset.workstation = "i2_w1";
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.asset_pid = "pid-auditAssetCannotBeAuditedBySameUserWhoDigitizedIt";
        asset.asset_locked = false;
        asset.status = "BEING_PROCESSED";
        assetService.persistAsset(asset, user, 10);
        assetService.completeAsset(new AssetUpdateRequest( new MinimalAsset("auditAssetCannotBeAuditedBySameUserWhoDigitizedIt", null, null, null),"i2_w1", "i2_p1", "bob"), user);
        DasscoIllegalActionException illegalActionException2 = assertThrows(DasscoIllegalActionException.class, () -> assetService.auditAsset(user, new Audit("Karl-Børge"), asset.asset_guid));
        assertThat(illegalActionException2).hasMessageThat().isEqualTo("Audit cannot be performed by the user who digitized the asset");
    }

    @Test
    void testDeleteAsset() {
        Asset createAsset = getTestAsset("deleteAsset");
        createAsset.pipeline = "i1_p1";
        createAsset.workstation = "i1_w1";
        createAsset.tags.put("Tag1", "value1");
        createAsset.tags.put("Tag2", "value2");
        createAsset.institution = "institution_1";
        createAsset.collection = "i1_c1";
        createAsset.asset_pid = "pid-deleteAsset";
        createAsset.status = "BEING_PROCESSED";
        assetService.persistAsset(createAsset, user,10);
        // Deleting returns true (no errors)
        assertThat(assetService.deleteAsset("deleteAsset", user)).isTrue();
        Optional<Asset> deleteAssetOpt = assetService.getAsset("deleteAsset");
        // Check that asset has not really been deleted, just added new event:
        assertThat(deleteAssetOpt.isPresent()).isTrue();
        Asset result = deleteAssetOpt.get();
        assertThat(result.date_asset_deleted).isNotNull();
        assertThat(result.events.get(0).event).isEqualTo(DasscoEvent.DELETE_ASSET_METADATA);
    }

    @Test
    void testDeleteAssetNoUser(){
        Asset asset = getTestAsset("deleteAssetNoUser");
        asset.pipeline = "i2_p1";
        asset.workstation = "i2_w1";
        asset.tags.put("Tag1", "value1");
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.asset_pid = "pid-deleteAssetNoUser";
        asset.status = "BEING_PROCESSED";
        assetService.persistAsset(asset, user, 10);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.deleteAsset("deleteAssetNoUser", new User()));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("User is null");
    }

    @Test
    void testDeleteAssetAssetDoesntExist(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.deleteAsset("non-existent-asset", user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset doesnt exist!");
    }

    @Test
    void testDeleteAssetAssetIsLocked(){
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
    void testDeleteAssetIsAlreadyDeleted(){
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
        assetService.unlockAsset(asset.asset_guid);
        Optional<Asset> unlockedAssetOpt = assetService.getAsset("lockUnlockAsset");
        assertThat(unlockedAssetOpt.isPresent()).isTrue();
        Asset unlockedAsset = unlockedAssetOpt.get();
        assertThat(unlockedAsset.asset_locked).isFalse();
    }

    @Test
    void testUnlockAssetAssetDoesntExist(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.unlockAsset("non-existent-asset"));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset doesnt exist!");
    }

    @Test
    void testGetEvents(){
        Asset asset = getTestAsset("assetEvents");
        asset.pipeline = "i2_p1";
        asset.workstation = "i2_w1";
        asset.tags.put("Tag1", "value1");
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.asset_pid = "pid-assetEvents";
        asset.status = "BEING_PROCESSED";
        assetService.persistAsset(asset, user, 10);
        List<Event> events = assetService.getEvents("assetEvents",user);
        assertThat(events.size()).isAtLeast(1);
        assertThat(events.get(0).event).isEqualTo(DasscoEvent.CREATE_ASSET_METADATA);
    }



    @Test
    void testCompleteAsset(){
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
        assertThat(assetService.completeAsset(new AssetUpdateRequest( new MinimalAsset("assetComplete", null, null, null),"i2_w1", "i2_p1", "bob"),user)).isTrue();
        Optional<Asset> optCompletedAsset = assetService.getAsset("assetComplete");
        assertThat(optCompletedAsset.isPresent()).isTrue();
        assertThat(optCompletedAsset.get().internal_status.toString()).isEqualTo("COMPLETED");
    }

    @Test
    void testCompleteAssetAssetDoesntExist(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.completeAsset(new AssetUpdateRequest( new MinimalAsset("non-existent-asset", null, null, null),"i1_w1", "i1_p1", "bob"), user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset doesnt exist!");
    }

    @Test
    void testCompleteUpload(){
        Asset asset = getTestAsset("assetUploadComplete");
        asset.pipeline = "i2_p1";
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
    void testCompleteUploadAssetIsNull(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.completeUpload(new AssetUpdateRequest(null,  "i2_w1", "i2_p1", "bob"), user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset cannot be null");
    }

    @Test
    void testCompleteUploadAssetDoesntExist(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.completeUpload(new AssetUpdateRequest( new MinimalAsset("non-existent-asset", null, null, null), "i2_w1", "i2_p1", "bob"), user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset doesnt exist!");
    }

    @Test
    void testCompleteUploadAssetIsLocked(){
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
        DasscoIllegalActionException dasscoIllegalActionException = assertThrows(DasscoIllegalActionException.class, () -> assetService.completeUpload(new AssetUpdateRequest( new MinimalAsset("completeUploadAssetIsLocked", null, null, null), "i2_w1", "i2_p1", "bob"), user));
        assertThat(dasscoIllegalActionException).hasMessageThat().isEqualTo("Asset is locked");
    }

    @Test
    void testSetAssetStatus(){
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
    void testSetAssetStatusInvalidStatus(){
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
    void testSetAssetStatusUnsupportedStatus(){
        Asset asset = getTestAsset("setAssetStatusUnsupportedStatus");
        asset.pipeline = "i2_p1";
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
    void testSetAssetAssetDoesntExist(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.setAssetStatus("non-existent-asset", "ERDA_ERROR", ""));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset doesnt exist!");
    }


    @Test
    void testValidateAssetFieldsNoAssetGuid(){
        Asset asset = new Asset();
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.validateAssetFields(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("asset_guid cannot be null");
    }

    @Test
    void testValidateAssetFieldsNoAssetPid(){
        Asset asset = new Asset();
        asset.asset_guid = "validateAssetFieldsNoAssetPid";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.validateAssetFields(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("asset_pid cannot be null");
    }

    @Test
    void testValidateAssetFieldsNoStatus(){
        Asset asset = new Asset();
        asset.asset_guid = "validateAssetFieldsNoStatus";
        asset.asset_pid = "pid-validateAssetFieldsNoStatus";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.validateAssetFields(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Status cannot be null");
    }

    @Test
    void testValidateAssetNoInstitution(){
        Asset asset = new Asset();
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.validateNewAssetAndSetIds(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Institution cannot be null");
    }

    @Test
    void testValidateAssetNoInstitutionDoesntExist(){
        Asset asset = new Asset();
        asset.institution = "doesnt exist";
        asset.collection = "collection_1";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.validateNewAssetAndSetIds(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Institution doesnt exist");
    }

    @Test
    void testValidateAssetCollectionDoesntExist(){
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.digitiser = "Bazviola";
        asset.collection = "doesnt ecksist";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.validateNewAssetAndSetIds(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Collection doesnt exist");
    }

    @Test
    void testValidateAssetNoCollection(){
        Asset asset = new Asset();
        asset.institution = "institution_2";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.validateNewAssetAndSetIds(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Collection cannot be null");
    }

    @Test
    void testValidateAssetNoPipeline(){
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.pipeline = "i50_p1250";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.validateAsset(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Pipeline doesnt exist in this institution");

    }

    @Test
    void testValidateAssetOwnParent(){
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.pipeline = "i2_p1";
        asset.asset_guid = "assetOwnParent";
        asset.parent_guid = "assetOwnParent";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.validateAsset(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset cannot be its own parent");
    }

    @Test
    void testValidateAssetNoWorkstation(){
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.collection = "i2_c1";
        asset.pipeline = "i2_p1";
        asset.asset_guid = "assetNoWorkstation";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.validateAsset(asset));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Workstation does not exist");
    }

    @Test
    void testValidateAssetWorkstationOutOfService(){
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
        Asset createAsset = getTestAsset("createAsset");
        createAsset.pipeline = "i1_p1";
        createAsset.workstation = "i1_w1";
        createAsset.tags.put("Tag1", "value1");
        createAsset.tags.put("Tag2", "value2");
        createAsset.institution = "institution_1";
        createAsset.specimens = Arrays.asList(new Specimen(createAsset.institution, "i1_c1", "creatAsset-sp-1", "spid1", "slide"), new Specimen(createAsset.institution, "i1_c1", "creatAsset-sp-2", "spid2", "pinning"));
        createAsset.collection = "i1_c1";
        createAsset.asset_pid = "pid-createAsset";
        createAsset.status = "BEING_PROCESSED";
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
        assertThat(result.parent_guid).isNull();
//        assertThat(result.specimen_barcodes).contains("createAsset-sp-1");
//        assertThat(result.specimen_barcodes).contains("createAsset-sp-2");
        assertThat(result.payload_type).isEqualTo("nuclear");
        assertThat(result.funding.get(0)).isEqualTo("Hundredetusindvis af dollars");
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

    @Test
    void testPersistAssetAllocationCannotBe0(){
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
    void testPersistAssetParentMustExist(){
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
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.persistAsset(asset, user, 10));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Parent doesnt exist");
    }

    @Test
    void testParentChildRelationIsNotDeletedWhenUpdatingParent(){
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
        asset2.parent_guid = asset.asset_guid;
        asset2.digitiser = "Bob";
        asset2.status = "BEING_PROCESSED";
        assetService.persistAsset(asset2, user, 10);
        Asset parent = assetService.getAsset(asset.asset_guid).get();
        parent.updateUser = "Bob";
        parent.funding = Arrays.asList("Hundredetusindvis af dollar, jeg er stadig i chok");
        assetService.updateAsset(parent, user);
        Asset child = assetService.getAsset(asset2.asset_guid).get();
        assertWithMessage("Parent should not be deleted").that(child.parent_guid).isNotEmpty();
    }

    @Test
    void testChangeParent(){
        Asset p1 = getTestAsset("testChangeParent_p1");
        Asset p2 = getTestAsset("testChangeParent_p2");
        Asset c = getTestAsset("testChangeParent_c");

        assetService.persistAsset(p1, user, 10);
        assetService.persistAsset(p2, user, 10);
        c.parent_guid = p1.asset_guid;
        assetService.persistAsset(c, user, 10);
        Asset cToUpdate = assetService.getAsset(c.asset_guid).get();
        cToUpdate.parent_guid = p2.asset_guid;
        cToUpdate.updateUser = user.username;
        assetService.updateAsset(cToUpdate,user);
        Asset cUpdated = assetService.getAsset(c.asset_guid).get();
        assertThat(cUpdated.parent_guid).isEqualTo(p2.asset_guid);
    }

    @Test
    void removeParentRelation(){
        Asset p1 = getTestAsset("removeParentRelation_p1");
        Asset c = getTestAsset("removeParentRelation_c");
        assetService.persistAsset(p1, user, 10);
        c.parent_guid = p1.asset_guid;
        assetService.persistAsset(c, user, 10);
        Asset cToUpdate = assetService.getAsset(c.asset_guid).get();
        assertThat(cToUpdate.parent_guid).isEqualTo(p1.asset_guid);
        cToUpdate.parent_guid = null;
        cToUpdate.updateUser = user.username;
        assetService.updateAsset(cToUpdate,user);
        Asset cUpdated = assetService.getAsset(c.asset_guid).get();
        assertThat(cUpdated.parent_guid).isNull();
    }

    @Test
    void testPersistAssetCannotSaveSameAssetTwice(){
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.workstation = "i2_w1";
        asset.pipeline = "i2_p1";
        asset.collection = "i2_c1";
        asset.asset_guid = "persistAssetCannotSaveSameAssetTwice";
        asset.asset_pid = "pid-persistAssetCannotSaveSameAssetTwice";
        asset.digitiser = "Karl-Børge";
        asset.status = "BEING_PROCESSED";
        assetService.persistAsset(asset, user, 1);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.persistAsset(asset, user, 1));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset persistAssetCannotSaveSameAssetTwice already exists");
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
        createAsset.status = "BEING_PROCESSED";
        assetService.persistAsset(createAsset, user,10);
        Asset asset = new Asset();
        asset.pipeline = "i1_p1";
        asset.workstation = "i1_w1";
        asset.institution = "institution_1";
        asset.collection = "i1_c1";
        asset.asset_pid = "createAssetUpdateWithMaxNull_pid";
        asset.asset_guid = "createAssetUpdateWithMaxNull";
        asset.updateUser = "thbo";
        asset.status ="ISSUE_WITH_METADATA";
        assetService.updateAsset(asset,user);
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
        createAsset.digitiser = "Basviola";
        createAsset.updateUser = "thbo";
        createAsset.status = "BEING_PROCESSED";
        assetService.persistAsset(createAsset, user,10);
        assetService.updateAsset(createAsset,user);
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
//        createAsset.status = "BEING_PROCESSED";
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
    void testGetAsset(){
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.workstation = "i2_w1";
        asset.pipeline = "i2_p1";
        asset.collection = "i2_c1";
        asset.asset_guid = "testGetAsset";
        asset.asset_pid = "pid-testGetAsset";
        asset.digitiser = "digigigizzzer";
        asset.status = "BEING_PROCESSED";
        assetService.persistAsset(asset, user, 1);
        Optional<Asset> optionalAsset = assetService.getAsset("testGetAsset");
        assertThat(optionalAsset.isPresent()).isTrue();
        Asset retrievedAsset = optionalAsset.get();
        assertThat(retrievedAsset.institution).isEqualTo("institution_2");
        assertThat(retrievedAsset.workstation).isEqualTo("i2_w1");
        assertThat(retrievedAsset.pipeline).isEqualTo("i2_p1");
        assertThat(retrievedAsset.collection).isEqualTo("i2_c1");
        assertThat(retrievedAsset.asset_guid).isEqualTo("testGetAsset");
        assertThat(retrievedAsset.asset_pid).isEqualTo("pid-testGetAsset");
        assertThat(retrievedAsset.status.toString()).isEqualTo("BEING_PROCESSED");
    }

    @Test
    void testDeleteAssetMetadata(){
        Asset asset = getTestAsset("test-delete-asset-metadata");
        asset.asset_pid = "test-delete-asset-metadata-pid";
        asset.status = "BEING_PROCESSED";
        asset.institution = "institution_2";
        asset.workstation = "i2_w1";
        asset.collection = "i2_c1";
        asset.pipeline = "i2_p1";
        assetService.persistAsset(asset, user, 1);
        assetService.deleteAssetMetadata(asset.asset_guid, user);
        Optional<Asset> optionalAsset = assetService.getAsset(asset.asset_guid);
        assertThat(optionalAsset.isPresent()).isFalse();
    }

    @Test
    void testDeleteAssetMetadataNonExistentAsset(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.deleteAssetMetadata("non-existent-asset", user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset doesnt exist!");
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
        return asset;
    }
}