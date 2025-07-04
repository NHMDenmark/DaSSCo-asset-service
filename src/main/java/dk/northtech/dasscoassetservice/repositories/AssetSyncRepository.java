package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.repositories.helpers.AssetMapper;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeListBuilder;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.apache.commons.text.StringSubstitutor;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.postgresql.jdbc.PgConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public interface AssetSyncRepository extends SqlObject {
    String completedAssetsSql =
            """
                SELECT * FROM ag_catalog.cypher('dassco', $$
                      MATCH (a:Asset {internal_status: 'COMPLETED'})
                      ${synced:-}
                      MATCH (c:Collection)<-[:IS_PART_OF]-(a)
                      MATCH (e:Event)<-[:CHANGED_BY]-(a)
                      WHERE e.event = 'CREATE_ASSET_METADATA'
                      MATCH (u:User)<-[:INITIATED_BY]-(e)
                      MATCH (i:Institution)<-[:BELONGS_TO]-(a)
                      OPTIONAL MATCH (p:Pipeline)<-[:USED]-(e)
                      OPTIONAL MATCH (w:Workstation)<-[:USED]-(e)
                      OPTIONAL MATCH (s:Specimen)-[sss:USED_BY]->(a)
                      OPTIONAL MATCH (a)-[:CHILD_OF]->(pa:Asset)

                       RETURN a.asset_guid
                           , a.asset_pid
                           , a.status
                           , a.multi_specimen
                           , a.funding
                           , a.subject
                           , a.payload_type
                           , a.file_formats
                           , a.asset_taken_date
                           , a.internal_status
                           , a.asset_locked
                           , pa.asset_guid AS parent_guid
                           , a.restricted_access
                           , a.tags
                           , a.error_message
                           , a.error_timestamp
                           , collect(s)
                           , i.name AS institution_name
                           , c.name AS collection_name
                           , p.name AS pipeline_name
                           , w.name AS workstation_name
                           , e.timestamp AS creation_date
                           , a.date_asset_finalised
                           , u.name AS user_name
                           , a.date_metadata_taken
                           , a.date_asset_taken
                           , false
                           , a.synced
                   $$)
               AS (asset_guid agtype
             , asset_pid agtype
             , status agtype
             , multi_specimen agtype
             , funding agtype
             , subject agtype
             , payload_type agtype
             , file_formats agtype
             , asset_taken_date agtype
             , internal_status agtype
             , asset_locked agtype
             , parent_guid agtype
             , restricted_access agtype
             , tags agtype
             , error_message agtype
             , error_timestamp agtype
             , specimens agtype
             , institution_name agtype
             , collection_name agtype
             , pipeline_name agtype
             , workstation_name agtype
             , creation_date agtype
             , date_asset_finalised agtype
             , user_name agtype
             , date_metadata_taken agtype
             , date_asset_taken agtype
             , write_access agtype
             , synced agtype);
            """;

    default void boilerplate() {
        withHandle(handle -> {
            Connection connection = handle.getConnection();
            try {
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                handle.execute("set search_path TO ag_catalog;");
                return handle;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    default List<Asset> getAllCompletedAssets(boolean filterSynced) {
        boilerplate();

        Map<String, String> substitutionMap = new HashMap<>();
        if (filterSynced) { // if we want to filter on synced assets only
            substitutionMap.put("synced", "WHERE a.synced = false");
        }
        StringSubstitutor substitutor = new StringSubstitutor(substitutionMap);

        String filteredQuery = substitutor.replace(completedAssetsSql);

        return withHandle(handle -> handle.createQuery(filteredQuery)
                .map(new AssetMapper())
                .list());
    }

    default List<String> setAssetsSynced(List<String> assetGuids) {
        String sql = """
                SELECT * FROM ag_catalog.cypher(
                          'dassco'
                      , $$
                            MATCH (a:Asset)
                            WHERE a.asset_guid IN $asset_guids
                            SET a.synced = true
                            RETURN a.asset_guid
                       $$
                    , #params
                  ) as (asset_guid agtype);
                """;

        return withHandle(handle -> {
            AgtypeListBuilder agtypeListBuilder = new AgtypeListBuilder();
            assetGuids.forEach(agtypeListBuilder::add);
            AgtypeMap agParams = new AgtypeMapBuilder()
                    .add("asset_guids", agtypeListBuilder)
                    .build();
            Agtype agtype = AgtypeFactory.create(agParams);
            return handle.createQuery(sql)
                    .bind("params", agtype)
                    .mapTo(String.class)
                    .list();
        });
    }

//    default Optional<Acknowledge> persistAcknowledge(Acknowledge acknowledge, String username) {
//        String sql = """
//            SELECT * FROM ag_catalog.cypher(
//                          'dassco'
//                      , $$
//                                MATCH (a:Asset)
//                                WHERE a.asset_guid IN $guids
//                                SET a.synced = true
//
//                                MERGE (e:Event {timestamp: $timestamp, event:'UPDATE_ASSET_METADATA', name: 'UPDATE_ASSET_METADATA'})
//                                MERGE (a)-[:CHANGED_BY]-(e)
//                                MERGE (e)-[:INITIATED_BY]->(u {name: $username})
//
//                                MERGE (es:Event {timestamp: $timestamp, event: 'ASSET_SYNCED', name: 'ASSET_SYNCED'})
//                                MERGE (a)-[:SYNCED]-(es)
//
//                                WITH DISTINCT e, es
//                                MERGE (ack:Acknowledge {asset_guids: $guids, status: $status, message: $message, date: $timestamp})-[:CHANGED_BY]-(es)
//                                RETURN ack.asset_guids, ack.status, ack.message, ack.date
//                           $$
//                    , #params
//                  ) as (asset_guids agtype, status agtype, body agtype, date agtype);
//                """;
//
////        return withHandle(handle -> {
////            AgtypeListBuilder agtypeListBuilder = new AgtypeListBuilder();
////            acknowledge.assetGuids().forEach(agtypeListBuilder::add);
////            AgtypeMapBuilder builder = new AgtypeMapBuilder()
////                    .add("guids", agtypeListBuilder)
////                    .add("timestamp", acknowledge.date().toEpochMilli())
////                    .add("status", acknowledge.status().toString())
////                    .add("body", acknowledge.message())
////                    .add("username", username);
////
////            Agtype agtype = AgtypeFactory.create(builder.build());
////            return handle.createQuery(sql)
////                    .bind("params", agtype)
////                    .map(new AcknowledgeMapper())
////                    .findOne();
////        });
//    }

    @SqlQuery("""
        SELECT asset_guid
        FROM asset
        WHERE asset.push_to_specify
            AND asset.asset_locked
            AND asset.internal_status = 'ERDA_SYNCHRONISED'
        """)
    Set<String> findAssetsForSpecifySync();
}
