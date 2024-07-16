package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.domain.Collection;
import org.apache.commons.compress.utils.Lists;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

class QueriesServiceTest extends AbstractIntegrationTest {
    User user = new User("moogie-woogie");
    User auditingUser = new User("moogie-auditor");

    @Test
    public void getNodeProperties() {
        Map<String, List<String>> nodes = queriesService.getNodeProperties();
        assertThat(nodes).isNotEmpty();
    }

    @Test
    public void unwrapQuery() {
        Optional<Institution> institution = institutionService.getIfExists("FNOOP");
        if (institution.isEmpty()) {
            institutionService.createInstitution(new Institution("FNOOP"));
            pipelineService.persistPipeline(new Pipeline("fnoopyline", "FNOOP"), "FNOOP");
            collectionService.persistCollection(new Collection("n_c1", "FNOOP", new ArrayList<>()));
            collectionService.persistCollection(new Collection("i_c1", "NNAD", new ArrayList<>()));
        }

        Asset firstAsset = getTestAsset("asset_fnoop_1", user.username, "FNOOP", "i2_w1", "fnoopyline", "n_c1");
        assetService.persistAsset(firstAsset, user, 11);

        Asset secondAsset = getTestAsset("asset_nnad_1", user.username, "NNAD", "i2_w1", "pl-01", "i_c1");
        assetService.persistAsset(secondAsset, user, 11);

        long yesterday = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli();
        long tomorrow = Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli();

        List<QueriesReceived> queries = new LinkedList<QueriesReceived>(Arrays.asList(
            new QueriesReceived("0", new LinkedList<Query>(Arrays.asList(
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
            new QueriesReceived("1", new LinkedList<Query>(Arrays.asList(
                new Query("Asset", new LinkedList<QueryWhere>(Arrays.asList(
                    new QueryWhere("asset_guid", Arrays.asList(
                            new QueryInner("CONTAINS", "nnad", QueryDataType.STRING)
                    ))
                )))
            )))
        ));

        List<Asset> assets = this.queriesService.getAssetsFromQuery(queries, 200);

        assertThat(assets.size()).isAtLeast(1);
        for (Asset asset : assets) {
            System.out.println(asset.event_name);
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

    @Test
    public void testingUserEvents() {
        Optional<Institution> institution = institutionService.getIfExists("FNOOP");
        if (institution.isEmpty()) {
            institutionService.createInstitution(new Institution("FNOOP"));
            pipelineService.persistPipeline(new Pipeline("fnoopyline", "FNOOP"), "FNOOP");
            collectionService.persistCollection(new Collection("n_c1", "FNOOP", new ArrayList<>()));
            collectionService.persistCollection(new Collection("i_c1", "NNAD", new ArrayList<>()));
        }
        Asset firstAsset = getTestAsset("asset_fnoop_2", user.username, "FNOOP", "i2_w1", "fnoopyline", "n_c1");
        assetService.persistAsset(firstAsset, user, 11);

        Asset secondAsset = getTestAsset("asset_nnad", user.username, "NNAD", "i2_w1", "pl-01", "i_c1");
        assetService.persistAsset(secondAsset, user, 11);

        Asset updatedAsest = secondAsset;
        updatedAsest.funding = "So much money it's insane";
        assetService.updateAsset(updatedAsest, user);
        updatedAsest.funding = "Even more money!!";
        assetService.updateAsset(updatedAsest, user);

        Asset auditedAsset = getTestAsset("audited", user.username, "NNAD", "i2_w1", "pl-01", "i_c1");
        auditedAsset.status = AssetStatus.BEING_PROCESSED;

        assetService.persistAsset(auditedAsset, user, 11);
        assetService.completeAsset(new AssetUpdateRequest("audited", new MinimalAsset("audited", null, "NNAD", "i_c1")
                , "i2_w1", "pl-01", user.username));
        assetService.auditAsset(auditingUser, new Audit(auditingUser.username), "audited");

        long tomorrow = Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli();

        List<QueriesReceived> queries2 = new LinkedList<QueriesReceived>(Arrays.asList(
            new QueriesReceived("0", new LinkedList<Query>(Arrays.asList(
                new Query("Asset", new LinkedList<QueryWhere>(Arrays.asList(
                    new QueryWhere("audited_timestamp", Arrays.asList(
                            new QueryInner("<=", Long.toString(tomorrow), QueryDataType.DATE)
                    )),
                    new QueryWhere("updated_timestamp", Arrays.asList(
                            new QueryInner("<=", Long.toString(tomorrow), QueryDataType.DATE)
                    )),
                    new QueryWhere("audited_by", Arrays.asList(
                            new QueryInner("=", "moogie-auditor", QueryDataType.STRING)
                    ))
                )))
            )))
        ));

        List<Asset> assets = this.queriesService.getAssetsFromQuery(queries2, 200);
        assertThat(assets.size()).isAtLeast(2);
        int asset_nnadCount = 0;
        for (Asset asset1 : assets) {
            if (asset1.asset_guid.equalsIgnoreCase("asset_nnad")) asset_nnadCount++;
        }
        boolean auditedFound = assets.stream().anyMatch(asset -> asset.asset_guid.equalsIgnoreCase("audited"));
        boolean notUpdatedAssetFound = assets.stream().anyMatch(asset -> asset.asset_guid.equalsIgnoreCase("asset_fnoop"));

        assertThat(asset_nnadCount).isEqualTo(1); // updated twice, so there'll be at least three events for this asset.
        assertThat(auditedFound).isTrue();
        assertThat(notUpdatedAssetFound).isFalse();
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