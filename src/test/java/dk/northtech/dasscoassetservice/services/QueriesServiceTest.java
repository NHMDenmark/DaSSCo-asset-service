package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.domain.Collection;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

class QueriesServiceTest extends AbstractIntegrationTest {
    User user = new User("moogie-woogie");

    @Test
    public void getNodeProperties() {
        Map<String, List<String>> nodes = queriesService.getNodeProperties();
        assertThat(nodes).isNotEmpty();
    }

    @Test
    public void unwrapQuery() {
        institutionService.createInstitution(new Institution("FNOOP"));
        pipelineService.persistPipeline(new Pipeline("fnoopyline", "FNOOP"), "FNOOP");
        collectionService.persistCollection(new Collection("n_c1", "FNOOP", new ArrayList<>()));
        collectionService.persistCollection(new Collection("i_c1", "NNAD", new ArrayList<>()));
        Asset firstAsset = getTestAsset("asset_fnoop");
        firstAsset.pipeline = "fnoopyline";
        firstAsset.workstation = "i2_w1";
        firstAsset.institution = "FNOOP";
        firstAsset.collection = "n_c1";
        firstAsset.asset_pid = "pid-auditAsset";
        firstAsset.asset_locked = false;
        firstAsset.status = AssetStatus.BEING_PROCESSED;
        assetService.persistAsset(firstAsset, user, 11);

        Asset secondAsset = getTestAsset("asset_nnad");
        secondAsset.pipeline = "pl-01";
        secondAsset.workstation = "i2_w1";
        secondAsset.institution = "NNAD";
        secondAsset.collection = "i_c1";
        secondAsset.asset_pid = "piddipiddy";
        secondAsset.asset_locked = false;
        secondAsset.status = AssetStatus.BEING_PROCESSED;
        assetService.persistAsset(secondAsset, user, 11);

        long yesterday = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli();
        long tomorrow = Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli();

        List<QueriesReceived> queries = new LinkedList<QueriesReceived>(Arrays.asList(
            new QueriesReceived(0, new LinkedList<Query>(Arrays.asList(
                new Query("Asset", new LinkedList<QueryWhere>(Arrays.asList(
                    new QueryWhere("asset_guid", Arrays.asList(
                            new QueryInner("CONTAINS", "fnoop", QueryDataType.STRING)
                    )),
                    new QueryWhere("created_timestamp", Arrays.asList(
                            new QueryInner("RANGE", yesterday + "#" + tomorrow, QueryDataType.DATE)
                    ))
                ))),
                new Query("Institution", new LinkedList<QueryWhere>(Arrays.asList(
                    new QueryWhere("name", Arrays.asList(
                            new QueryInner("CONTAINS", "FNOOP", QueryDataType.STRING)
                    ))
                ))),
                new Query("Pipeline", new LinkedList<QueryWhere>(Arrays.asList(
                    new QueryWhere("name", Arrays.asList(
                            new QueryInner("STARTS WITH", "fnoop", QueryDataType.STRING)
                    ))
                )))
            ))),
            new QueriesReceived(1, new LinkedList<Query>(Arrays.asList(
                new Query("Asset", new LinkedList<QueryWhere>(Arrays.asList(
                    new QueryWhere("asset_guid", Arrays.asList(
                            new QueryInner("CONTAINS", "nnad", QueryDataType.STRING)
                    ))
                )))
            )))
        ));

        List<Asset> assets = this.queriesService.unwrapQuery(queries, 200);

        assertThat(assets.size()).isAtLeast(1);
        for (Asset asset : assets) {
            if (asset.asset_guid.equalsIgnoreCase(firstAsset.asset_guid)) {
                assertThat(asset.institution).matches(firstAsset.institution);
                assertThat(asset.pipeline).matches(firstAsset.pipeline);
            }
            if (asset.asset_guid.equalsIgnoreCase(secondAsset.asset_guid)) {
                assertThat(asset.institution).matches(secondAsset.institution);
                assertThat(asset.pipeline).matches(secondAsset.pipeline);
            }
        }
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

    @Test
    public void getAndSaveSearch() {
        List<SavedQuery> savedQueries = this.queriesService.getSavedQueries(user.username);

        String query = "[{\"id\": 0, \"query\": [{ \"select\": \"Asset\", \"where\": [{ \"property\": \"name\", \"fields\": " +
                "[{ \"operator\": \"CONTAINS\", \"value\": \"7\", \"dataType\": \"STRING\" }, { \"operator\": \"CONTAINS\", " +
                "\"value\": \"5\", \"dataType\": \"STRING\" }]} ]}, { \"select\": \"Institution\", \"where\": " +
                "[{ \"property\": \"name\", \"fields\": [{ \"operator\": \"CONTAINS\", \"value\": \"test\", \"dataType\": " +
                "\"STRING\" }]}]}]}]";

        SavedQuery savedQuery = new SavedQuery("Asset 1", query);
        SavedQuery saved = this.queriesService.saveQuery(savedQuery, user.username);
        List<SavedQuery> savedQueriesAfter = this.queriesService.getSavedQueries(user.username);

        assertThat(savedQueries.size()).isLessThan(savedQueriesAfter.size());
        assertThat(saved.name).isEqualTo(savedQuery.name);
    }

    @Test
    public void updateSavedSearch() {
        String query = "[{\"id\": 0, \"query\": [{ \"select\": \"Asset\", \"where\": [{ \"property\": \"name\", \"fields\": " +
                "[{ \"operator\": \"CONTAINS\", \"value\": \"7\", \"dataType\": \"STRING\" }, { \"operator\": \"CONTAINS\", " +
                "\"value\": \"5\", \"dataType\": \"STRING\" }]} ]}, { \"select\": \"Institution\", \"where\": " +
                "[{ \"property\": \"name\", \"fields\": [{ \"operator\": \"CONTAINS\", \"value\": \"test\", \"dataType\": " +
                "\"STRING\" }]}]}]}]";

        String updatedQuery = "[{\"id\": 0, \"query\": [{ \"select\": \"Asset\", \"where\": [{ \"property\": \"name\", \"fields\": " +
                "[{ \"operator\": \"CONTAINS\", \"value\": \"3\", \"dataType\": \"STRING\" }, { \"operator\": \"CONTAINS\", " +
                "\"value\": \"1\", \"dataType\": \"STRING\" }]} ]}, { \"select\": \"Institution\", \"where\": " +
                "[{ \"property\": \"name\", \"fields\": [{ \"operator\": \"CONTAINS\", \"value\": \"test\", \"dataType\": " +
                "\"STRING\" }]}]}]}]";

        SavedQuery initialQuery = new SavedQuery("Asset 1", query);
        this.queriesService.saveQuery(initialQuery, user.username);
        List<SavedQuery> savedQueriesBeforeUpdate = this.queriesService.getSavedQueries(user.username);

        SavedQuery updatedSavedQuery = this.queriesService.updateSavedQuery("Asset 1", new SavedQuery("New title!", updatedQuery), user.username);
        List<SavedQuery> savedQueriesAfterUpdate = this.queriesService.getSavedQueries(user.username);

        assertThat(savedQueriesBeforeUpdate.contains(initialQuery)).isTrue();
        assertThat(savedQueriesAfterUpdate).isNotEmpty();
        assertThat(savedQueriesAfterUpdate.contains(initialQuery)).isFalse();
        assertThat(savedQueriesAfterUpdate.contains(updatedSavedQuery)).isTrue();
    }

    @Test
    public void deleteSavedSearch() {
        String query = "[{\"id\": 0, \"query\": [{ \"select\": \"Asset\", \"where\": [{ \"property\": \"name\", \"fields\": " +
                "[{ \"operator\": \"CONTAINS\", \"value\": \"7\", \"dataType\": \"STRING\" }, { \"operator\": \"CONTAINS\", " +
                "\"value\": \"5\", \"dataType\": \"STRING\" }]} ]}, { \"select\": \"Institution\", \"where\": " +
                "[{ \"property\": \"name\", \"fields\": [{ \"operator\": \"CONTAINS\", \"value\": \"test\", \"dataType\": " +
                "\"STRING\" }]}]}]}]";

        SavedQuery initialQuery = new SavedQuery("Asset 1", query);
        this.queriesService.saveQuery(initialQuery, user.username);
        List<SavedQuery> savedQueriesBeforeUpdate = this.queriesService.getSavedQueries(user.username);

        String deletedQueryName = this.queriesService.deleteSavedQuery("Asset 1", user.username);
        List<SavedQuery> savedQueriesAfterUpdate = this.queriesService.getSavedQueries(user.username);

        assertThat(deletedQueryName).matches("\"" + "Asset 1" + "\"");
        assertThat(savedQueriesBeforeUpdate.contains(initialQuery)).isTrue();
        assertThat(savedQueriesAfterUpdate.size()).isLessThan(savedQueriesBeforeUpdate.size());
        assertThat(savedQueriesAfterUpdate.contains(initialQuery)).isFalse();
    }
}