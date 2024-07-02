package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.domain.Collection;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

import java.time.Instant;
import java.util.*;

class QueriesServiceTest extends AbstractIntegrationTest {
    User user = new User("Teztuzer");

    @Test
    public void getNodeProperties() {
        Map<String, List<String>> nodes = queriesService.getNodeProperties();
        System.out.println(nodes);
        assertThat(nodes).isNotEmpty();
    }

    @Test
    public void unwrapQuery() {
        institutionService.createInstitution(new Institution("NNAD"));
        pipelineService.persistPipeline(new Pipeline("queryPipeline", "NNAD"), "NNAD");
        collectionService.persistCollection(new Collection("n_c1", "NNAD", new ArrayList<>()));
        Asset asset = getTestAsset("auditAssetQueries");
        asset.pipeline = "queryPipeline";
        asset.workstation = "i2_w1";
        asset.institution = "NNAD";
        asset.collection = "n_c1";
        asset.asset_pid = "pid-auditAsset";
        asset.asset_locked = false;
        asset.status = AssetStatus.BEING_PROCESSED;

        assetService.persistAsset(asset, user, 11);

        List<QueryField> assWheres = Arrays.asList(
            new QueryField("and", "=", "asset_guid", "auditAssetQueries")
        );
        List<QueryField> instWheres = Arrays.asList(
            new QueryField("and", "=", "name", "NNAD")
        );
        List<QueryField> pipelineWheres = Arrays.asList(
            new QueryField("and", "=", "name", "queryPipeline")
        );
        Query query = new Query("Asset", assWheres);
        Query query2 = new Query("Institution", instWheres);
        Query query3 = new Query("Pipeline", pipelineWheres);
        List<Query> queries = new ArrayList<>();
        queries.add(query);
        queries.add(query2);
        queries.add(query3);
        List<Asset> assets = this.queriesService.unwrapQuery(queries, 200);

        assertThat(assets.size()).isEqualTo(1);
        assertThat(assets.get(0).asset_guid).isEqualTo("auditAssetQueries");
        assertThat(assets.get(0).institution).isEqualTo("NNAD");
        assertThat(assets.get(0).digitiser).isEqualTo("Karl-Børge");
        assertThat(assets.get(0).pipeline).isEqualTo("queryPipeline");

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