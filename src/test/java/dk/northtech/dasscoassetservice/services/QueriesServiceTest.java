package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.domain.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Disabled("Disabled until wp5a is prioritized")
class QueriesServiceTest extends AbstractIntegrationTest {
    User user = new User("moogie-woogie");
    User auditingUser = new User("moogie-auditor");


    @BeforeEach
    void ensureUserExists() {
        jdbi.useHandle(handle -> {
            handle.createUpdate("""
                INSERT INTO dassco_user (username, keycloak_id, dassco_user_id)
                VALUES (:username, 'a854889a-6fa2-4f4e-89e9-2b74ce866b82', 1)
                ON CONFLICT (username) DO NOTHING;
            """)
                    .bind("username", user.username)
                    .execute();
        });
    }

    @Test
    public void getNodeProperties() {
        List<QueryItem> nodes = queriesService.getNodeProperties();
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

        List<QueryResultAsset> assets = this.queriesService.getAssetsFromQuery(queries, 200, user);

        for (QueryResultAsset asset : assets) {
            if (asset.asset_guid().equalsIgnoreCase(firstAsset.asset_guid)) {
                assertThat(asset.institution()).matches(firstAsset.institution);
//                assertThat(asset.pipeline).matches(firstAsset.pipeline);
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
        childAsset.parent_guids = Set.of(parentAsset.asset_guid);
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

        List<QueryResultAsset> assets = this.queriesService.getAssetsFromQuery(emptyChildQuery, 200, user);
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

        List<QueryResultAsset> childAssets = this.queriesService.getAssetsFromQuery(childQuery, 200, user);
        assertThat(childAssets.size()).isAtLeast(1);
        boolean parentFound = childAssets.stream().anyMatch(asset -> asset.asset_guid().equalsIgnoreCase(parentAsset.asset_guid));
        boolean childFound = childAssets.stream().anyMatch(asset -> asset.asset_guid().equalsIgnoreCase(childAsset.asset_guid));
        boolean standardFound = childAssets.stream().anyMatch(asset -> asset.asset_guid().equalsIgnoreCase(normalAsset.asset_guid));
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

        List<QueryResultAsset> assets = this.queriesService.getAssetsFromQuery(queries2, 200, user);
        assertThat(assets.size()).isAtLeast(2);
        int asset_nnadCount = 0;
        for (QueryResultAsset asset1 : assets) {
            if (asset1.asset_guid().equalsIgnoreCase("asset_nnad")) asset_nnadCount++;
        }
        boolean auditedFound = assets.stream().anyMatch(asset -> asset.asset_guid().equalsIgnoreCase("audited"));
        boolean notUpdatedAssetFound = assets.stream().anyMatch(asset -> asset.asset_guid().equalsIgnoreCase("asset_fnoop"));
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
                                new QueryWhere("collection", Arrays.asList(
                                        new QueryInner("CONTAINS", "closed", QueryDataType.STRING)
                                ))
                        )))
                )))
        ));
        List<QueryResultAsset> assets = this.queriesService.getAssetsFromQuery(queryCollectionWhere, 200, user);

        assertThat(assets.size()).isEqualTo(1);
        assertThat(assets.get(0).asset_guid()).isEqualTo("asset_4");
        assertThat(assets.get(0).collection()).isEqualTo("CLOSED_coll");

        List<QueriesReceived> queriesNoCollectionWhere = new LinkedList<QueriesReceived>(Arrays.asList(
                new QueriesReceived("0", new LinkedList<Query>(Arrays.asList(
                        new Query("Asset", new LinkedList<QueryWhere>(Arrays.asList(
                                new QueryWhere("asset_guid", Arrays.asList(
                                        new QueryInner("CONTAINS", "a", QueryDataType.STRING)
                                ))
                        )))
                )))
        ));

        List<QueryResultAsset> assets2 = this.queriesService.getAssetsFromQuery(queriesNoCollectionWhere, 200, user);
        int assets3 = this.queriesService.getAssetCountFromQuery(queriesNoCollectionWhere, 200, user);

        boolean WOOP_coll_closedNotExist = assets2.stream().anyMatch(asset -> asset.collection().equalsIgnoreCase("WOOP_coll_closed"));
        assertThat(WOOP_coll_closedNotExist).isFalse();

        boolean CLOSED_collExists = assets2.stream().anyMatch(asset -> asset.collection().equalsIgnoreCase("CLOSED_coll"));
        assertThat(CLOSED_collExists).isTrue();

        boolean WOOP_coll_openExists = assets2.stream().anyMatch(asset -> asset.collection().equalsIgnoreCase("WOOP_coll_open"));
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
        asset.asset_subject = "Folder";
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

        SavedQuery initialQuery = this.queriesService.saveQuery(new SavedQuery("Asset 1", query), user.username);
        List<SavedQuery> savedQueriesBeforeUpdate = this.queriesService.getSavedQueries(user.username);

        SavedQuery updatedSavedQuery = this.queriesService.updateSavedQuery(initialQuery.name, new SavedQuery("New title!", updatedQuery),  user.username);
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

        SavedQuery initialQuery = this.queriesService.saveQuery(new SavedQuery("Asset 1", query), user.username);
        List<SavedQuery> savedQueriesBeforeUpdate = this.queriesService.getSavedQueries(user.username);
        String deletedQueryName = this.queriesService.deleteSavedQuery(initialQuery.name, user.username);
        List<SavedQuery> savedQueriesAfterUpdate = this.queriesService.getSavedQueries(user.username);

        assertThat(deletedQueryName).isEqualTo("Asset 1");
        assertThat(savedQueriesBeforeUpdate.contains(initialQuery)).isTrue();
        assertThat(savedQueriesAfterUpdate.size()).isLessThan(savedQueriesBeforeUpdate.size());
        assertThat(savedQueriesAfterUpdate.contains(initialQuery)).isFalse();
    }
}
