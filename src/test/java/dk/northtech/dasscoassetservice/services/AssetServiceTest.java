package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssetServiceTest extends AbstractIntegrationTest {

    @Inject
    AssetService assetService;

    User user = new User();
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
        assetService.persistAsset(createAsset, user);
        //Check that the same asset cannot be added multiple times
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.persistAsset(createAsset, user));
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
        assertThat(result.internal_status).isEqualTo(InternalStatus.SMB_ERROR);
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
//        child.parent_guid = createAsset.guid;
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
        assetService.persistAsset(createAsset, user);
        assetService.deleteAsset("Karl-Børge", "deleteAsset");
        Optional<Asset> deleteAssetOpt = assetService.getAsset("deleteAsset");
        Asset result = deleteAssetOpt.get();
        assertThat(result.asset_deleted_date).isNotNull();
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
        assetService.persistAsset(createAsset, user);
        Optional<Asset> resultOpt = assetService.getAsset("createAssetUpdateAsset");
        assertThat(resultOpt.isPresent()).isTrue();
        Asset result = resultOpt.get();
        result.updateUser = "Uffe Updater";
        result.payload_type = "conventional";
        assetService.updateAsset(result);
        result.payload_type = "nuclear";
        assetService.updateAsset(result);
        assetService.completeAsset(new AssetUpdateRequest(null, new MinimalAsset("createAssetUpdateAsset", null),"i1_w1", "i1_p1", "bob"));
        assetService.auditAsset(new Audit("Audrey Auditor"), "createAssetUpdateAsset");
        List<Event> resultEvents = assetService.getEvents(result.asset_guid);
        assertThat(resultEvents.size()).isEqualTo(5);
        Optional<Asset> resultOpt2 = assetService.getAsset("createAssetUpdateAsset");
        Asset resultAsset = resultOpt2.get();
        assertThat(resultAsset.payload_type).isEqualTo("nuclear");
        Instant latestUpdate;
        List<Instant> updates = resultEvents.stream().filter(x -> x.event.equals(DasscoEvent.UPDATE_ASSET_METADATA)).map(x -> x.timeStamp).sorted().collect(Collectors.toList());
        //The last update event
        assertThat(resultAsset.last_updated_date).isEqualTo(updates.get(1));
        assertThat(resultAsset.audited).isTrue();
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
        assetService.persistAsset(asset, user);
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
        assetService.persistAsset(asset, user);
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
        assetService.persistAsset(asset, user);
        DasscoIllegalActionException illegalActionException1 = assertThrows(DasscoIllegalActionException.class, () -> assetService.auditAsset(new Audit("Karl-Børge"), asset.asset_guid));
        assertThat(illegalActionException1).hasMessageThat().isEqualTo("Asset must be complete before auditing");
//        assetService.completeAsset(asset.asset_guid);
        assetService.completeAsset(new AssetUpdateRequest(null, new MinimalAsset("auditAsset", null),"i2_w1", "i2_p1", "bob"));
        DasscoIllegalActionException illegalActionException2 = assertThrows(DasscoIllegalActionException.class, () -> assetService.auditAsset(new Audit("Karl-Børge"), asset.asset_guid));
        assertThat(illegalActionException2).hasMessageThat().isEqualTo("Audit cannot be performed by the user who digitized the asset");
    }

    public Asset getTestAsset(String guid) {
        Asset asset = new Asset();
        asset.asset_locked = false;
        asset.digitiser = "Karl-Børge";
        asset.asset_guid = guid;
        asset.funding = "Hundredetusindvis af dollars";
        asset.asset_taken_date = Instant.now();
        asset.subject = "Folder";
        asset.file_formats = Arrays.asList(FileFormat.JPEG);
        asset.payload_type = "nuclear";
        asset.updateUser = "Basviola";
        return asset;
    }
}