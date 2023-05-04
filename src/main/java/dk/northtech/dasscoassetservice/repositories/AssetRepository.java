package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.repositories.helpers.AssetMapper;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import joptsimple.internal.Strings;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeListBuilder;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.sqlobject.CreateSqlObject;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.postgresql.jdbc.PgConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

//@Repository
public interface AssetRepository extends SqlObject {
//    private Jdbi jdbi;
//    private DataSource dataSource;

    @CreateSqlObject
    SpecimenRepository createSpecimenRepository();

    default void boilerplate() {
        withHandle(handle -> {
            Connection connection = handle.getConnection();
            try {
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                handle.execute(DBConstants.AGE_BOILERPLATE);
                return handle;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Transaction
    default Asset createAsset(Asset asset) {
        boilerplate();
        persistAsset(asset);
        createSpecimenRepository().persistSpecimens(asset);
        connectParentChild(asset.parent_guid, asset.guid);
        return asset;
    }

    @Transaction
    default Optional<Asset> readAsset(String assetId) {
        boilerplate();
        return readAssetInternal(assetId);
    }

    default Optional<Asset> readAssetInternal(String assetGuid) {
        String sql = """
                SELECT * FROM ag_catalog.cypher(
                'dassco'
                    , $$
                         MATCH (a:Asset{name: $guid})
                         MATCH (c:Collection)<-[:IS_PART_OF]-(a)
                         MATCH (e:Event{event:'CREATE_ASSET'})<-[:CHANGED_BY]-(a)
                         MATCH (p:Pipeline)<-[:USED]-(e)
                         MATCH (w:Workstation)<-[:USED]-(e)
                         MATCH (i:Institution)<-[:BELONGS_TO]-(a)
                         OPTIONAL MATCH (s:Specimen)-[sss:USED_BY]->(:Asset{name: $guid})
                         OPTIONAL MATCH (a)-[:CHILD_OF]->(pa:Asset)
                         RETURN a.guid
                         , a.pid
                         , a.status
                         , a.multi_specimen
                         , a.funding, a.subject
                         , a.payload_type
                         , a.file_formats
                         , a.asset_taken_date
                         , a.internal_status
                         , pa.guid
                         , a.restricted_access
                         , a.tags
                         , collect(s.name)
                         , i.name
                         , c.name
                         , p.name
                         , w.name
                         , e.timestamp
                         , a.pushed_to_specify_date
                      $$
                    , #params)
                    as (guid agtype
                    , pid agtype
                    , status agtype
                    , multi_specimen agtype
                    , funding agtype
                    , subject agtype
                    , payload_type agtype
                    , file_formats agtype
                    , asset_taken_date agtype
                    , internal_status agtype
                    , parent_guid agtype
                    , restricted_access agtype
                    , tags agtype
                    , specimen_barcodes agtype
                    , institution_name agtype
                    , collection_name agtype
                    , pipeline_name agtype
                    , workstation_name agtype
                    , creation_date agtype
                    , pushed_to_specify_date agtype);
                  """;
        return withHandle(handle -> {
            AgtypeMap agParams = new AgtypeMapBuilder()
                    .add("guid", assetGuid)
                    .build();
            Agtype agtype = AgtypeFactory.create(agParams);
            return handle.createQuery(sql)
                    .bind("params", agtype)
                    .map(new AssetMapper())
                    .findOne();
        });
    }

    default void connectParentChild(String parentGuid, String childGuid) {
        if(Strings.isNullOrEmpty(parentGuid)) {
            return;
        }
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (p:Asset {name: $pGuid})
                            MATCH (c:Asset {name: $cGuid})
                            MERGE (c)-[cf:CHILD_OF]->(p)
                        $$
                        , #params) as (ag agtype);
                        """;
        withHandle(handle -> {

        AgtypeMap parentChildRelation = new AgtypeMapBuilder()
                .add("pGuid", parentGuid)
                .add("cGuid", childGuid).build();
        Agtype specimenEdge = AgtypeFactory.create(parentChildRelation);
        handle.createUpdate(sql)
                .bind("params", specimenEdge)
                .execute();
        return handle;
        });
    }
    default Asset persistAsset(Asset asset) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (i:Institution {name: $institution_name})
                            MATCH (c:Collection {name: $collection_name})
                            MATCH (w:Workstation {name: $workstation_name})
                            MATCH (p:Pipeline {name: $pipeline_name})
                            MERGE (a:Asset {name: $guid
                                , pid: $pid
                                , guid: $guid
                                , status: $status
                                , funding: $funding
                                , subject: $subject
                                , payload_type: $payload_type
                                , file_formats: $file_formats
                                , asset_taken_date: $asset_taken_date
                                , internal_status: $internal_status
                                , restricted_access: $restricted_access
                                , tags: $tags
                            })
                            MERGE (u:User{user_id: $user, name: $user})
                            MERGE (e:Event{timestamp: $created_date, event:'CREATE_ASSET', name: 'CREATE_ASSET'})
                            MERGE (e)-[uw:USED]->(w)
                            MERGE (e)-[up:USED]->(p)
                            MERGE (e)-[pb:INITIATED_BY]->(u)
                            MERGE (a)-[ca:CHANGED_BY]-(e)    
                            MERGE (a)-[bt:BELONGS_TO]->(i)
                            MERGE (w)-[sa:STATIONED_AT]->(i)
                            MERGE (p)-[ub:USED_BY]->(i)
                            MERGE (a)-[ipf:IS_PART_OF]->(c)
                        $$
                        , #params) as (a agtype);
                        """;
        try {
            withHandle(handle -> {
                AgtypeListBuilder agtypeListBuilder = new AgtypeListBuilder();
                asset.file_formats.forEach(x -> agtypeListBuilder.add(x.name()));
                AgtypeMapBuilder tags = new AgtypeMapBuilder();
                asset.tags.entrySet().forEach(tag -> tags.add(tag.getKey(), tag.getValue())); //(tag -> tags.add(tag));
                AgtypeListBuilder restrictedAcces = new AgtypeListBuilder();
                asset.restricted_access.forEach(role -> restrictedAcces.add(role.name()));
                AgtypeMap parms = new AgtypeMapBuilder()
                        .add("institution_name", asset.institution)
                        .add("collection_name", asset.collection)
                        .add("workstation_name", asset.workstation)
                        .add("pipeline_name", asset.pipeline)
                        .add("pid", asset.pid)
                        .add("guid", asset.guid)
                        .add("status", asset.status.name())
                        .add("funding", asset.funding)
                        .add("subject", asset.subject)
                        .add("payload_type", asset.payload_type)
                        .add("file_formats", agtypeListBuilder.build())
                        .add("asset_taken_date", asset.asset_taken_date != null ? DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")).format(asset.asset_taken_date) : null)
                        .add("created_date", DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")).format(asset.created_date))
                        .add("internal_status", asset.internal_status.name())
                        .add("parent_id",asset.parent_guid)
                        .add("user", asset.digitizer)
                        .add("tags",tags.build())
                        .add("restricted_access", restrictedAcces.build())
                        .build();
                Agtype agtype = AgtypeFactory.create(parms);
                handle.createUpdate(sql)
                        .bind("params", agtype)
                        .execute();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return asset;
    }
}
