package dk.northtech.dasscoassetservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.domain.Collection;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static com.google.common.truth.Truth.assertThat;

class AssetSyncServiceTest extends AbstractIntegrationTest {
    User user = new User("moogie-woogie");

    @Test
    public void adapter() {

    }

    @Disabled
    @Test
    public void specify() {
        Optional<Institution> institution = institutionService.getIfExists("FNOOP");
        if (institution.isEmpty()) {
            institutionService.createInstitution(new Institution("FNOOP"));
            pipelineService.persistPipeline(new Pipeline("fnoopyline", "FNOOP"), "FNOOP");
            collectionService.persistCollection(new Collection("n_c1", "FNOOP", new ArrayList<>()));
            collectionService.persistCollection(new Collection("i_c1", "NNAD", new ArrayList<>()));
        }
        Asset asset1 = getTestAsset("queue_asset_1", user.username, "FNOOP", "i2_w1", "fnoopyline", "n_c1");
        Asset asset2 = getTestAsset("queue_asset_2_exit", user.username, "FNOOP", "i2_w1", "fnoopyline", "n_c1");
        assetService.persistAsset(asset1, user, 1);
        assetService.persistAsset(asset2, user, 1);
        List<Asset> assets = new ArrayList<>();
        assets.add(asset1);
        assets.add(asset2);

        ObjectWriter ow = new ObjectMapper().registerModule(new JavaTimeModule()).writer().withDefaultPrettyPrinter();
        try {
            String json = ow.writeValueAsString(assets);
            int s = specifyAdapterClient.sendAssets(json, user);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("An error occurred when trying to turn the Assets into JSON for the queue.", e);
        }
    }

    @Disabled
    @Test
    public void testQueueSynchronisation() {
        Optional<Institution> institution = institutionService.getIfExists("FNOOP");
        if (institution.isEmpty()) {
            institutionService.createInstitution(new Institution("FNOOP"));
            pipelineService.persistPipeline(new Pipeline("fnoopyline", "FNOOP"), "FNOOP");
            collectionService.persistCollection(new Collection("n_c1", "FNOOP", new ArrayList<>()));
            collectionService.persistCollection(new Collection("i_c1", "NNAD", new ArrayList<>()));
        }
        Asset asset1 = getTestAsset("queue_asset_1", user.username, "FNOOP", "i2_w1", "fnoopyline", "n_c1");
        Asset asset2 = getTestAsset("queue_asset_2_exit", user.username, "FNOOP", "i2_w1", "fnoopyline", "n_c1");
        assetService.persistAsset(asset1, user, 1);
        assetService.persistAsset(asset2, user, 1);

        assetService.completeAsset(new AssetUpdateRequest("share1", new MinimalAsset("queue_asset_1", null, null, null), "i2_w1", "i2_p1", "bob"));
        assetService.completeAsset(new AssetUpdateRequest("share1", new MinimalAsset("queue_asset_2_exit", null, null, null), "i2_w1", "i2_p1", "bob"));

        assetSyncService.syncAssets(false);
//        queueBroadcaster.sendMessage();
    }

    @Test
    public void testCompletedAssets() {
        Optional<Institution> institution = institutionService.getIfExists("FNOOP");
        if (institution.isEmpty()) {
            institutionService.createInstitution(new Institution("FNOOP"));
            pipelineService.persistPipeline(new Pipeline("fnoopyline", "FNOOP"), "FNOOP");
            collectionService.persistCollection(new Collection("n_c1", "FNOOP", new ArrayList<>()));
            collectionService.persistCollection(new Collection("i_c1", "NNAD", new ArrayList<>()));
        }

        Asset asset1 = getTestAsset("synced_asset_complete_1", user.username, "FNOOP", "i2_w1", "fnoopyline", "n_c1");
        assetService.persistAsset(asset1, user, 1);
        Asset asset2 = getTestAsset("synced_asset_complete_2", user.username, "FNOOP", "i2_w1", "fnoopyline", "n_c1");
        assetService.persistAsset(asset2, user, 1);

        assetService.completeAsset(new AssetUpdateRequest("share1", new MinimalAsset("synced_asset_complete_1", null, null, null), "i2_w1", "i2_p1", "bob"));
        assetService.completeAsset(new AssetUpdateRequest("share1", new MinimalAsset("synced_asset_complete_2", null, null, null), "i2_w1", "i2_p1", "bob"));

        Optional<Asset> optAsset1 = assetService.getAsset("synced_asset_complete_1");
        assertThat(optAsset1.isPresent()).isTrue();
        assertThat(optAsset1.get().internal_status.toString()).isEqualTo("COMPLETED");

        Optional<Asset> optAsset2 = assetService.getAsset("synced_asset_complete_2");
        assertThat(optAsset2.isPresent()).isTrue();
        assertThat(optAsset2.get().internal_status.toString()).isEqualTo("COMPLETED");

        List<Asset> completed = assetSyncService.getAllCompletedAssets();

        assertThat(completed.stream().anyMatch(asset -> asset.asset_guid.equalsIgnoreCase(asset1.asset_guid))).isTrue();
        assertThat(completed.stream().anyMatch(asset -> asset.asset_guid.equalsIgnoreCase(asset2.asset_guid))).isTrue();

        assetSyncService.setAssetsSynced(List.of(asset1.asset_guid));

        List<Asset> unsyncedCompleted = assetSyncService.getAllUnsyncedCompletedAssets();

        assertThat(unsyncedCompleted.stream().anyMatch(asset -> asset.asset_guid.equalsIgnoreCase(asset1.asset_guid))).isFalse();
        assertThat(unsyncedCompleted.stream().anyMatch(asset -> asset.asset_guid.equalsIgnoreCase(asset2.asset_guid))).isTrue();
    }

    public Asset getTestAsset(String guid, String username, String institution, String workstation, String pipeline, String collection) {
        Asset asset = new Asset();
        asset.asset_locked = false;
        asset.digitiser = username;
        asset.asset_guid = guid;
        asset.funding = "Hundredetusindvis af dollars";
        asset.date_asset_taken = Instant.now();
        asset.subject = "Folder";
        asset.file_formats = Arrays.asList(FileFormat.JPEG);
        asset.payload_type = "nuclear";
        asset.updateUser = username;
        asset.pipeline = pipeline;
        asset.workstation = workstation;
        asset.institution = institution;
        asset.collection = collection;
        asset.asset_pid = "pid-auditAsset";
        asset.asset_locked = false;
        asset.status = AssetStatus.BEING_PROCESSED;
        return asset;
    }
}