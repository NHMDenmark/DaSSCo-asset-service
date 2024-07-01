package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
        institutionService.createInstitution(new Institution("FNOOP"));
        pipelineService.persistPipeline(new Pipeline("queryPipeline", "NNAD"), "NNAD");
        pipelineService.persistPipeline(new Pipeline("fnoopyline", "FNOOP"), "FNOOP");

        Asset asset = getTestAsset("asset_nnad_to_fnoop");
        asset.pipeline = "queryPipeline";
        asset.workstation = "i2_w1";
        asset.institution = "NNAD";
        asset.collection = "i1_c2";
        asset.asset_pid = "pid-auditAsset";
        asset.asset_locked = false;
        asset.status = AssetStatus.BEING_PROCESSED;
        Asset firstAsset = assetService.persistAsset(asset, user, 11);

        firstAsset.asset_pid = "pidpid";
        firstAsset.institution = "FNOOP";
        firstAsset.pipeline = "fnoopyline";
        Asset updatedAsset = assetService.updateAsset(firstAsset);

        System.out.println(updatedAsset.asset_guid);
        System.out.println(updatedAsset.institution);
        System.out.println(updatedAsset.pipeline);
        System.out.println(updatedAsset.created_date);
        System.out.println(updatedAsset.date_metadata_updated);

        Asset secondAsset = getTestAsset("asset_nnad");
        secondAsset.pipeline = "queryPipeline";
        secondAsset.workstation = "i2_w1";
        secondAsset.institution = "NNAD";
        secondAsset.collection = "i1_c2";
        secondAsset.asset_pid = "piddipiddy";
        secondAsset.asset_locked = false;
        secondAsset.status = AssetStatus.BEING_PROCESSED;
        assetService.persistAsset(secondAsset, user, 11);

        System.out.println("new");
        System.out.println(secondAsset.created_date);
        long yesterday = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli();
        long tomorrow = Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli();

        List<QueriesReceived> queries = new LinkedList<QueriesReceived>(Arrays.asList(
            new QueriesReceived(0, new LinkedList<QueryV2>(Arrays.asList(
                new QueryV2("Asset", new LinkedList<QueryWhere>(Arrays.asList(
                    new QueryWhere("asset_guid", Arrays.asList(
                            new QueryInner("CONTAINS", "fnoop")
                    )),
                    new QueryWhere("updated_timestamp", Arrays.asList(
                            new QueryInner("RANGE", yesterday + "#" + tomorrow)
                    )),
                    new QueryWhere("created_timestamp", Arrays.asList(
                            new QueryInner("RANGE", yesterday + "#" + tomorrow)
                    ))
                ))),
                new QueryV2("Institution", new LinkedList<QueryWhere>(Arrays.asList(
                    new QueryWhere("name", Arrays.asList(
                            new QueryInner("CONTAINS", "FN")
                    ))
                ))),
                new QueryV2("Pipeline", new LinkedList<QueryWhere>(Arrays.asList(
                    new QueryWhere("name", Arrays.asList(
                            new QueryInner("ENDS WITH", "ine")
                    ))
                )))
            ))),
            new QueriesReceived(1, new LinkedList<QueryV2>(Arrays.asList(
                new QueryV2("Asset", new LinkedList<QueryWhere>(Arrays.asList(
                    new QueryWhere("asset_guid", Arrays.asList(
                            new QueryInner("CONTAINS", "fnoop")
                    ))
//                    new QueryWhere("asset_guid", Arrays.asList(
//                        new QueryInner("=", "ASSEt_NnaD")
//                    )),
//                    new QueryWhere("created_timestamp", Arrays.asList(
//                            new QueryInner(">=", String.valueOf(yesterday))
//                    ))
                )))
//                    ,
//                new QueryV2("Institution", new LinkedList<QueryWhere>(Arrays.asList(
//                    new QueryWhere("name", Arrays.asList(
//                            new QueryInner("=", "NnAD")
//                    ))
//                ))),
//                new QueryV2("Pipeline", new LinkedList<QueryWhere>(Arrays.asList(
//                    new QueryWhere("name", Arrays.asList(
//                            new QueryInner("CONTAINS", "fnoop")
//                    ))
//                )))
            )))
        ));

        List<Asset> assets = this.queriesService.unwrapQuery(queries, 200);
        for (Asset asse : assets) {
            System.out.println(asse.asset_guid);
            System.out.println(asse.institution);
            System.out.println(asse.pipeline);
        }

//        assertThat(assets.size()).isEqualTo(1);
//        assertThat(assets.get(0).asset_guid).isEqualTo("auditAssetQueries");
//        assertThat(assets.get(0).institution).isEqualTo("NNAD");
//        assertThat(assets.get(0).digitiser).isEqualTo("Karl-Børge");
//        assertThat(assets.get(0).pipeline).isEqualTo("queryPipeline");

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