package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.domain.Collection;
import org.apache.commons.compress.utils.Lists;
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

    @Disabled
    @Test
    public void testingUserEvents() {
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

        Asset updatedAsest = secondAsset;
        updatedAsest.funding = "So much money it's insane";

        assetService.updateAsset(updatedAsest, user);

        updatedAsest.funding = "Even more money!!";
        assetService.updateAsset(updatedAsest, user);

        Asset toDeleteAsset = getTestAsset("asset_deleting");
        toDeleteAsset.pipeline = "pl-01";
        toDeleteAsset.workstation = "i2_w1";
        toDeleteAsset.institution = "NNAD";
        toDeleteAsset.collection = "i_c1";
        toDeleteAsset.asset_pid = "piddipiddy";
        toDeleteAsset.asset_locked = false;
        toDeleteAsset.status = AssetStatus.BEING_PROCESSED;
        assetService.persistAsset(toDeleteAsset, user, 11);

        boolean deleted = assetService.deleteAsset("asset_deleting", user);
        System.out.println("deleted: " + deleted);

        Asset auditedAsset = getTestAsset("audited");
        auditedAsset.pipeline = "pl-01";
        auditedAsset.workstation = "i2_w1";
        auditedAsset.institution = "NNAD";
        auditedAsset.collection = "i_c1";
        auditedAsset.asset_pid = "piddipiddy";
        auditedAsset.asset_locked = false;
        auditedAsset.status = AssetStatus.BEING_PROCESSED;

        assetService.persistAsset(auditedAsset, user, 11);
        assetService.completeAsset(new AssetUpdateRequest("audited", new MinimalAsset("audited", null, "NNAD", "i_c1")
                , "i2_w1", "pl-01", user.username));
        assetService.auditAsset(auditingUser, new Audit(auditingUser.username), "audited");

        long yesterday = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli();
        long twodaysago = Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli();
        long tomorrow = Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli();

        List<QueriesReceived> queries = new LinkedList<QueriesReceived>(Arrays.asList(
            new QueriesReceived("0", new LinkedList<Query>(Arrays.asList(
                new Query("Asset", new LinkedList<QueryWhere>(Arrays.asList(
                    new QueryWhere("asset_guid", Arrays.asList(
                            new QueryInner("=", "asset_nnad", QueryDataType.STRING)
                    )),
                    new QueryWhere("updated_timestamp", Arrays.asList(
                            new QueryInner("RANGE", twodaysago + "#" + tomorrow, QueryDataType.DATE)
                    )),
                    new QueryWhere("asset_created_by", Arrays.asList(
                            new QueryInner("=", "moogie-woogie", QueryDataType.STRING)
                    )),
                    new QueryWhere("asset_updated_by", Arrays.asList(
                            new QueryInner("=", "moogie-woogie", QueryDataType.STRING)
                    )),
                    new QueryWhere("created_timestamp", Arrays.asList(
                            new QueryInner("RANGE", yesterday + "#" + tomorrow, QueryDataType.DATE)
                    ))
                )))
            )))
        ));

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
//                    new QueryWhere("updated_timestamp", Arrays.asList(
//                            new QueryInner("RANGE", twodaysago + "#" + tomorrow, QueryDataType.DATE)
//                    )),
//                    new QueryWhere("asset_created_by", Arrays.asList(
//                            new QueryInner("=", "moogie-woogie", QueryDataType.STRING)
//                    )),
//                    new QueryWhere("asset_updated_by", Arrays.asList(
//                            new QueryInner("=", "moogie-woogie", QueryDataType.STRING)
//                    ))
                )))
//                new Query("Asset", new LinkedList<QueryWhere>(Arrays.asList(
//                    new QueryWhere("audited_by", Arrays.asList(
//                            new QueryInner("=", "moogie-auditor", QueryDataType.STRING)
//                    ))
//                )))
            )))
        ));

        List<Asset> assets = this.queriesService.getAssetsFromQuery(queries2, 200);
//        List<Asset> set = new HashSet<>(assets).stream().toList();
//        System.out.println(assets);
//        for (Asset asset : assets) {
//            System.out.println(asset.toString());
//        }
        for (Asset asset : assets) {
            System.out.println(asset.toString());
        }
    }

    public Asset getTestAsset(String guid) {
        Asset asset = new Asset();
        asset.asset_locked = false;
        asset.digitiser = user.username;
        asset.asset_guid = guid;
        asset.funding = "Hundredetusindvis af dollars";
        asset.date_asset_taken = Instant.now();
        asset.subject = "Folder";
        asset.file_formats = Arrays.asList(FileFormat.JPEG);
        asset.payload_type = "nuclear";
        asset.updateUser = user.username;
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