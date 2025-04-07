package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.domain.Collection;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.text.StringSubstitutor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Map.entry;

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

        Asset secondAsset = getTestAsset("asset_fnoop_2", user.username, "FNOOP", "i2_w1", "fnoopyline", "n_c1");
        assetService.persistAsset(secondAsset, user, 11);

        Asset thirdAsset = getTestAsset("asset_nnad_1", user.username, "NNAD", "i2_w1", "pl-01", "i_c1");
        assetService.persistAsset(thirdAsset, user, 11);

        List<QueriesReceived> queries = new LinkedList<>(List.of(
                new QueriesReceived("0", new LinkedList<>(Arrays.asList(
                        new Query("Asset", new LinkedList<>(Arrays.asList(
                                new QueryWhere("asset_guid", Arrays.asList(
                                        new QueryInner("CONTAINS", "e", QueryDataType.STRING),
                                        new QueryInner("CONTAINS", "a", QueryDataType.STRING)
                                )),
                                new QueryWhere("asset_guid", List.of(
                                        new QueryInner("ENDS WITH", "1", QueryDataType.STRING)
                                )),
                                new QueryWhere("status", List.of(
                                        new QueryInner("=", "BEING_PROCESSED", QueryDataType.STRING)
                                ))
                        ))),
                        new Query("Pipeline", new LinkedList<>(List.of(
                                new QueryWhere("name", List.of(
                                        new QueryInner("=", "fnoopyline", QueryDataType.STRING)
                                ))
                        )))
                )))
        ));

        List<Asset> assets = this.queriesService.getAssetsFromQuery(queries, 200, user);

        for (Asset asset : assets) {
            if (asset.asset_guid.equalsIgnoreCase(firstAsset.asset_guid)) {
                assertThat(asset.institution).matches(firstAsset.institution);
                assertThat(asset.pipeline).matches(firstAsset.pipeline);
            }
        }
    }

    @Test
    public void testParentGuidQuery() {
        Optional<Institution> institution = institutionService.getIfExists("FNOOP");
        if (institution.isEmpty()) {
            institutionService.createInstitution(new Institution("FNOOP"));
            pipelineService.persistPipeline(new Pipeline("fnoopyline", "FNOOP"), "FNOOP");
            collectionService.persistCollection(new Collection("n_c1", "FNOOP", new ArrayList<>()));
            collectionService.persistCollection(new Collection("i_c1", "NNAD", new ArrayList<>()));
        }

        Asset parentAsset = getTestAsset("asset_parent", user.username, "FNOOP", "i2_w1", "fnoopyline", "n_c1");
        assetService.persistAsset(parentAsset, user, 11);
        Asset childAsset = getTestAsset("asset_child", user.username, "FNOOP", "i2_w1", "fnoopyline", "n_c1");
        childAsset.parent_guid = parentAsset.asset_guid;
        assetService.persistAsset(childAsset, user, 11);
        Asset normalAsset = getTestAsset("asset_standard", user.username, "FNOOP", "i2_w1", "fnoopyline", "n_c1");
        assetService.persistAsset(normalAsset, user, 11);

        List<QueriesReceived> emptyChildQuery = new LinkedList<QueriesReceived>(Arrays.asList(
            new QueriesReceived("0", new LinkedList<Query>(Arrays.asList(
                new Query("Asset", new LinkedList<QueryWhere>(Arrays.asList(
                    new QueryWhere("asset_guid", Arrays.asList(
                            new QueryInner("CONTAINS", "standard", QueryDataType.STRING)
                    )),
                    new QueryWhere("parent_guid", Arrays.asList(
                            new QueryInner("CONTAINS", "parent", QueryDataType.STRING)
                    ))
                )))
            )))
        ));

        List<Asset> assets = this.queriesService.getAssetsFromQuery(emptyChildQuery, 200, user);
        assertThat(assets.size()).isEqualTo(0);

        List<QueriesReceived> childQuery = new LinkedList<QueriesReceived>(Arrays.asList(
                new QueriesReceived("0", new LinkedList<Query>(Arrays.asList(
                        new Query("Asset", new LinkedList<QueryWhere>(Arrays.asList(
                                new QueryWhere("asset_guid", Arrays.asList(
                                        new QueryInner("CONTAINS", "child", QueryDataType.STRING)
                                )),
                                new QueryWhere("parent_guid", Arrays.asList(
                                        new QueryInner("CONTAINS", "parent", QueryDataType.STRING)
                                ))
                        )))
                )))
        ));

        List<Asset> childAssets = this.queriesService.getAssetsFromQuery(childQuery, 200, user);
        assertThat(childAssets.size()).isAtLeast(1);
        boolean parentFound = childAssets.stream().anyMatch(asset -> asset.asset_guid.equalsIgnoreCase(parentAsset.asset_guid));
        boolean childFound = childAssets.stream().anyMatch(asset -> asset.asset_guid.equalsIgnoreCase(childAsset.asset_guid));
        boolean standardFound = childAssets.stream().anyMatch(asset -> asset.asset_guid.equalsIgnoreCase(normalAsset.asset_guid));
        assertThat(parentFound).isFalse();
        assertThat(standardFound).isFalse();
        assertThat(childFound).isTrue();
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
        Asset firstAsset = getTestAsset("asset_fnoop_3", user.username, "FNOOP", "i2_w1", "fnoopyline", "n_c1");
        assetService.persistAsset(firstAsset, user, 11);

        Asset secondAsset = getTestAsset("asset_nnad", user.username, "NNAD", "i2_w1", "pl-01", "i_c1");
        assetService.persistAsset(secondAsset, user, 11);

        Asset updatedAsest = secondAsset;
        updatedAsest.funding = Arrays.asList("So much money it's insane");
        assetService.updateAsset(updatedAsest, user);
        updatedAsest.funding = Arrays.asList("Even more money!!");
        assetService.updateAsset(updatedAsest, user);

        Asset auditedAsset = getTestAsset("audited", user.username, "NNAD", "i2_w1", "pl-01", "i_c1");
        auditedAsset.status = "BEING_PROCESSED";

        assetService.persistAsset(auditedAsset, user, 11);
        assetService.completeAsset(new AssetUpdateRequest( new MinimalAsset("audited", null, "NNAD", "i_c1")
                , "i2_w1", "pl-01", user.username),user);
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

        List<Asset> assets = this.queriesService.getAssetsFromQuery(queries2, 200, user);
        assertThat(assets.size()).isAtLeast(2);
        int asset_nnadCount = 0;
        for (Asset asset1 : assets) {
            System.out.println(asset1);
            if (asset1.asset_guid.equalsIgnoreCase("asset_nnad")) asset_nnadCount++;
        }
        boolean auditedFound = assets.stream().anyMatch(asset -> asset.asset_guid.equalsIgnoreCase("audited"));
        boolean notUpdatedAssetFound = assets.stream().anyMatch(asset -> asset.asset_guid.equalsIgnoreCase("asset_fnoop"));
        while(true) {
            try {
                Thread.sleep(400000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
//        assertThat(asset_nnadCount).isEqualTo(1); // updated twice, so there'll be at least three events for this asset.
//        assertThat(auditedFound).isTrue();
//        assertThat(notUpdatedAssetFound).isFalse();
    }

    @Test
    public void testAccess() {
        user.roles = new HashSet<>(Arrays.asList("READ_CLOSED_INST_USER", "READ_WOOP_USER", "READ_CLOSED_coll_USER")); // NOT allowed on WOOP.WOOP_coll_closed_USER collection.
        List<QueriesReceived> queryCollectionWhere = new LinkedList<QueriesReceived>(Arrays.asList(
                new QueriesReceived("0", new LinkedList<Query>(Arrays.asList(
                        new Query("Asset", new LinkedList<QueryWhere>(Arrays.asList(
                                new QueryWhere("asset_guid", Arrays.asList(
                                        new QueryInner("CONTAINS", "a", QueryDataType.STRING)
                                ))
                        ))),
                        new Query("Collection", new LinkedList<QueryWhere>(Arrays.asList(
                                new QueryWhere("name", Arrays.asList(
                                        new QueryInner("CONTAINS", "closed", QueryDataType.STRING)
                                ))
                        )))
                )))
        ));
        List<Asset> assets = this.queriesService.getAssetsFromQuery(queryCollectionWhere, 200, user);

        assertThat(assets.size()).isEqualTo(1);
        assertThat(assets.get(0).asset_guid).isEqualTo("asset_4");
        assertThat(assets.get(0).collection).isEqualTo("CLOSED_coll");

        List<QueriesReceived> queriesNoCollectionWhere = new LinkedList<QueriesReceived>(Arrays.asList(
                new QueriesReceived("0", new LinkedList<Query>(Arrays.asList(
                        new Query("Asset", new LinkedList<QueryWhere>(Arrays.asList(
                                new QueryWhere("asset_guid", Arrays.asList(
                                        new QueryInner("CONTAINS", "a", QueryDataType.STRING)
                                ))
                        )))
                )))
        ));

        List<Asset> assets2 = this.queriesService.getAssetsFromQuery(queriesNoCollectionWhere, 200, user);
        int assets3 = this.queriesService.getAssetCountFromQuery(queriesNoCollectionWhere, 200, user);

        boolean WOOP_coll_closedNotExist = assets2.stream().anyMatch(asset -> asset.collection.equalsIgnoreCase("WOOP_coll_closed"));
        assertThat(WOOP_coll_closedNotExist).isFalse();

        boolean CLOSED_collExists = assets2.stream().anyMatch(asset -> asset.collection.equalsIgnoreCase("CLOSED_coll"));
        assertThat(CLOSED_collExists).isTrue();

        boolean WOOP_coll_openExists = assets2.stream().anyMatch(asset -> asset.collection.equalsIgnoreCase("WOOP_coll_open"));
        assertThat(WOOP_coll_openExists).isTrue();

        assertThat(assets2.size()).isEqualTo(assets3);
    }

    public Asset getTestAsset(String guid, String username, String institution, String workstation, String pipeline, String collection) {
        Asset asset = new Asset();
        asset.asset_locked = false;
        asset.digitiser = username;
        asset.asset_guid = guid;
        asset.funding = Arrays.asList("Hundredetusindvis af dollars");
        asset.date_asset_taken = Instant.now();
        asset.subject = "Folder";
        asset.file_formats = Arrays.asList("JPEG");
        asset.payload_type = "nuclear";
        asset.updateUser = username;
        asset.pipeline = pipeline;
        asset.workstation = workstation;
        asset.institution = institution;
        asset.collection = collection;
        asset.asset_pid = "pid-auditAsset";
        asset.asset_locked = false;
        asset.status = "BEING_PROCESSED";
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
