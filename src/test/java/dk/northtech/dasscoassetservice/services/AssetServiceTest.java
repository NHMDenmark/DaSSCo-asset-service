package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssetServiceTest extends AbstractIntegrationTest {

    @Inject
    AssetService assetService;

    @Test
    void createAsset() {
        Asset createAsset = getTestAsset("createAsset");
        createAsset.specimen_barcodes = Arrays.asList("createAsset-sp-1", "createAsset-sp-2");
        createAsset.pipeline = "i1_p1";
        createAsset.workstation = "i1_w1";
        createAsset.tags.put("Tag1", "value1");
        createAsset.tags.put("Tag2", "value2");
        createAsset.institution = "institution_1";
        createAsset.collection = "i1_c1";
        createAsset.pid = "pid-createAsset";
        createAsset.status = AssetStatus.BEING_PROCESSED;
        assetService.persistAsset(createAsset);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> assetService.persistAsset(createAsset));
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
        assertThat(result.digitizer).isEqualTo("Karl-Børge");
        assertThat(result.internal_status).isEqualTo(InternalStatus.METADATA_RECEIVED);
        assertThat(result.parent_guid).isNull();
        assertThat(result.specimen_barcodes).contains("createAsset-sp-1");
        assertThat(result.specimen_barcodes).contains("createAsset-sp-2");
        assertThat(result.payload_type).isEqualTo("nuclear");
        assertThat(result.funding).isEqualTo("Hundredetusindvis af dollars");
    }

    @Test
    void createAssetUpdateAsset() {
        Asset createAsset = getTestAsset("createAssetUpdateAsset");
        createAsset.specimen_barcodes = Arrays.asList("createAssetUpdateAsset-sp-1");
        createAsset.pipeline = "i1_p1";
        createAsset.workstation = "i1_w1";
        createAsset.tags.put("Tag1", "value1");
        createAsset.tags.put("Tag2", "value2");
        createAsset.institution = "institution_1";
        createAsset.collection = "i1_c1";
        createAsset.pid = "pid-createAsset";
        createAsset.status = AssetStatus.BEING_PROCESSED;
        assetService.persistAsset(createAsset);
        Optional<Asset> resultOpt = assetService.getAsset("createAssetUpdateAsset");
        assertThat(resultOpt.isPresent()).isTrue();
        Asset result = resultOpt.get();
        result.payload_type = "conventional";
        assetService.updateAsset(result);
        result.payload_type = "nuclear";
        assetService.updateAsset(result);
        assetService.completeAsset("createAssetUpdateAsset");
        assetService.auditAsset(new Audit("Audrey Auditor"), "createAssetUpdateAsset");
        List<Event> resultEvents = assetService.getEvents(result.guid);
        assertThat(resultEvents.size()).isEqualTo(4);
        Optional<Asset> resultOpt2 = assetService.getAsset("createAssetUpdateAsset");
        Asset resultAsset = resultOpt2.get();
        assertThat(resultAsset.payload_type).isEqualTo("nuclear");
        resultEvents.forEach(x -> System.out.println(x.timeStamp));
        //The last update event
        assertThat(resultAsset.last_updated_date).isEqualTo(resultEvents.get(1).timeStamp);
        assertThat(resultAsset.audited).isTrue();

    }

    @Test
    void updateAsset() {
        Asset asset = getTestAsset("updateAsset");
        asset.specimen_barcodes = Arrays.asList("createAsset-sp-1", "createAsset-sp-2");
        asset.pipeline = "i1_p1";
        asset.workstation = "i1_w1";
        asset.tags.put("Tag1", "value1");
        asset.tags.put("Tag2", "value2");
        asset.institution = "institution_1";
        asset.collection = "i1_c1";
        asset.pid = "pid-updateAsset";
        asset.status = AssetStatus.BEING_PROCESSED;
        assetService.persistAsset(asset);
        asset.tags.remove("Tag1");
        asset.tags.remove("Tag2");
        asset.workstation = "i1_w2";
        asset.pipeline = "i1_p2";
        asset.status = AssetStatus.ISSUE_WITH_METADATA;
        asset.subject = "new sub";
        asset.restricted_access = Arrays.asList(Role.ADMIN);
        asset.funding = "Funding secured";
        asset.file_formats = Arrays.asList(FileFormat.RAW);
        asset.payload_type = "Conventional";
        asset.digitizer = "Diane Digitiser";
        assetService.updateAsset(asset);
        Optional<Asset> updateAsset = assetService.getAsset("updateAsset");
        assertThat(updateAsset.isPresent()).isTrue();
        Asset result = updateAsset.get();
        assertThat(result.tags.isEmpty()).isTrue();
        // The pipeline and workstation fields on asset is the ones used to create the assets.
        // The ones set on the updated asset is used on the update event and is not displayed on the assete
        assertThat(result.pipeline).isEqualTo("i1_p1");
        assertThat(result.workstation).isEqualTo("i1_w1");
        assertThat(result.status).isEqualTo(AssetStatus.ISSUE_WITH_METADATA);
        assertThat(result.subject).isEqualTo("new sub");
        assertThat(result.restricted_access.get(0)).isEqualTo(Role.ADMIN);
        assertThat(result.funding).isEqualTo("Funding secured");
        assertThat(result.file_formats.size()).isEqualTo(1);
        assertThat(result.file_formats.get(0)).isEqualTo(FileFormat.RAW);
        assertThat(result.payload_type).isEqualTo("Conventional");
        //Digitizer is the original creator of the asset. The name of the new digitizer appears on the update event in the graph
        assertThat(result.digitizer).isEqualTo("Karl-Børge");
    }

    @Test
    void lockUnlockAsset() {
        Asset asset = getTestAsset("lockUnlockAsset");
        asset.specimen_barcodes = Arrays.asList("createAsset-sp-1", "createAsset-sp-2");
        asset.pipeline = "i1_p2";
        asset.workstation = "i1_w2";
        asset.institution = "institution_1";
        asset.collection = "i1_c2";
        asset.pid = "pid-lockUnlock";
        asset.asset_locked = true;
        asset.status = AssetStatus.BEING_PROCESSED;
        assetService.persistAsset(asset);
        Optional<Asset> lockedAssetOpt = assetService.getAsset("lockUnlockAsset");
        Asset lockedAsset = lockedAssetOpt.get();
        assertThat(lockedAsset.asset_locked).isTrue();
        assetService.unlockAsset(asset.guid);
        Optional<Asset> unlockedAssetOpt = assetService.getAsset("lockUnlockAsset");
        Asset unlockedAsset = unlockedAssetOpt.get();
        assertThat(unlockedAsset.asset_locked).isFalse();

    }

    @Test
    void auditAsset() {
        Asset asset = getTestAsset("auditAsset");
        asset.specimen_barcodes = Arrays.asList("auditAsset-sp-1");
        asset.pipeline = "i2_p1";
        asset.workstation = "i2_w1";
        asset.institution = "institution_2";
        asset.collection = "i1_c2";
        asset.pid = "pid-auditAsset";
        asset.asset_locked = false;
        asset.status = AssetStatus.BEING_PROCESSED;
        assetService.persistAsset(asset);
        DasscoIllegalActionException illegalActionException1 = assertThrows(DasscoIllegalActionException.class, () -> assetService.auditAsset(new Audit("Karl-Børge"), asset.guid));
        assertThat(illegalActionException1).hasMessageThat().isEqualTo("Asset must be complete before auditing");
        assetService.completeAsset(asset.guid);
        DasscoIllegalActionException illegalActionException2 = assertThrows(DasscoIllegalActionException.class, () -> assetService.auditAsset(new Audit("Karl-Børge"), asset.guid));
        assertThat(illegalActionException2).hasMessageThat().isEqualTo("Audit cannot be performed by the user who digitized the asset");
    }

    public Asset getTestAsset(String guid) {
        Asset asset = new Asset();
        asset.asset_locked = false;
        asset.digitizer = "Karl-Børge";
        asset.guid = guid;
        asset.funding = "Hundredetusindvis af dollars";
        asset.asset_taken_date = Instant.now();
        asset.subject = "Folder";
        asset.file_formats = Arrays.asList(FileFormat.JPEG);
        asset.payload_type = "nuclear";
        return asset;
    }
}