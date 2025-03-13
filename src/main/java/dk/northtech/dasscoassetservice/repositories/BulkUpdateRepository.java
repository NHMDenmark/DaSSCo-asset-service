package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.DasscoEvent;
import dk.northtech.dasscoassetservice.domain.Event;
import dk.northtech.dasscoassetservice.domain.Specimen;
import dk.northtech.dasscoassetservice.repositories.helpers.AssetMapper;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import dk.northtech.dasscoassetservice.repositories.helpers.EventMapper;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static dk.northtech.dasscoassetservice.repositories.AssetRepository2.READ_WITHOUT_WHERE;


//@Repository
public interface BulkUpdateRepository extends SqlObject {
    //    private Jdbi jdbi;
//    private DataSource dataSource;
    static final Logger logger = LoggerFactory.getLogger(BulkUpdateRepository.class);

    @CreateSqlObject
    SpecimenRepository createSpecimenRepository();

    //This must be called once per transaction
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
    default List<Asset> readMultipleAssets(List<String> assets) {
        boilerplate();
        return readMultipleAssetsInternal(assets);
    }


    @Transaction
    default List<Asset> bulkUpdate(String sql, AgtypeMapBuilder builder, Asset updatedAsset, Event event, List<Asset> assets, List<String> assetList) {
        boilerplate();
        // Update asset metadata:
        bulkUpdateAssets(sql, builder);
        // Add Event to every asset:
        // TODO: This is a solution for the bulk update, but it takes individual calls.
        for (Asset asset : assets) {
            // Set event (individual calls)
            setEvent(updatedAsset.updateUser, event, asset);
            // Connect parent and child (individual calls)
            connectParentChild(updatedAsset.parent_guid, asset.asset_guid);
            // Modify tags
            if (!updatedAsset.tags.isEmpty()) {
                setTags(asset);
            }
        }

        // Return the List of Assets:
        return this.readMultipleAssetsInternal(assetList);
    }




    @Transaction
    default List<Event> readEvents(String guid) {
        boilerplate();
        return readEvents_internal(guid);
    }

    default List<Event> readEvents_internal(String guid) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (e:Event)<-[:CHANGED_BY]-(a:Asset{name: $asset_guid})
                            MATCH (u:User)<-[:INITIATED_BY]-(e)
                            OPTIONAL MATCH (p:Pipeline)<-[:USED]-(e)
                            OPTIONAL MATCH (w:Workstation)<-[:USED]-(e)
                            RETURN e.timestamp
                                , e.event
                                , u.name
                                , p.name
                                , w.name
                        $$
                        , #params) 
                        as (timestamp agtype
                            , event agtype
                            , event_user agtype
                            , pipeline agtype
                            , workstation agtype);
                        """;

        return withHandle(handle -> {
            AgtypeMap agParams = new AgtypeMapBuilder()
                    .add("asset_guid", guid)
                    .build();
            Agtype agtype = AgtypeFactory.create(agParams);
            List<Event> events = handle.createQuery(sql)
                    .bind("params", agtype)
                    .map(new EventMapper())
                    .list();

            events.sort(Collections.reverseOrder(Comparator.comparing(event -> event.timeStamp)));
            return events;
        });
    }

    default void connectParentChild(String parentGuid, String childGuid) {
        if (Strings.isNullOrEmpty(parentGuid)) {
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


    default List<Asset> readMultipleAssetsInternal(List<String> assets) {
        String sql = """
                  SELECT * FROM ag_catalog.cypher(
                'dassco'
                    , $$
                         MATCH (asset:Asset)
                         WHERE asset.asset_guid IN $asset_guids
                         """
                            + READ_WITHOUT_WHERE
;
        return withHandle(handle -> {
            AgtypeListBuilder assetGuidList = new AgtypeListBuilder();
            assets.forEach(assetGuidList::add);
            AgtypeMap agParams = new AgtypeMapBuilder()
                    .add("asset_guids", assetGuidList.build() )
                    .build();
            Agtype agtype = AgtypeFactory.create(agParams);
            return handle.createQuery(sql)
                    .bind("params", agtype)
                    .map(new AssetMapper())
                    .list();
        });
    }



    static String buildUpdateSQL(Asset asset, AgtypeMapBuilder agBuilder) {
        StringBuilder sb = new StringBuilder("""
                SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (collection:Collection {name: $collection_name})
                            MATCH (workstation:Workstation {name: $workstation_name})
                            MATCH (pipeline:Pipeline {name: $pipeline_name})
                            MATCH (asset:Asset {name: $asset_guid})
                            MATCH (asset)-[existing_has_status:HAS]->(status:Status)
                            MATCH (new_status:Status{name:$status})
                            OPTIONAL MATCH (asset)-[existing_child_of:CHILD_OF]->(parent:Asset)
                            OPTIONAL MATCH (asset)<-[existing_worked_on:WORKED_ON]-(:Digitiser)
                            OPTIONAL MATCH (asset)<-[existing_digitised:DIGITISED]-(:Digitiser)
                            OPTIONAL MATCH (asset)<-[existing_funds:FUNDS]-(:Funding_entity)
                            OPTIONAL MATCH (asset)-[existing_has_payload_type:HAS]->(payload_type:Payload_type)
                            OPTIONAL MATCH (asset)-[existing_has_subject:HAS]->(subject:Subject)
                            OPTIONAL MATCH (asset)-[existing_has_camera_setting_control:HAS]->(camera_setting_control:Camera_setting_control)
                            OPTIONAL MATCH (asset)-[existing_has_metadata_version:HAS]->(metadata_version:Metadata_version)
                            OPTIONAL MATCH (asset)-[existing_has_metadata_source:HAS]->(metadata_source:Metadata_source)
                            OPTIONAL MATCH (asset)-[existing_has_file_format:HAS]->(file_format:File_format)
                            DELETE existing_digitised
                            DELETE existing_worked_on
                            DELETE existing_has_payload_type
                            DELETE existing_has_subject
                            DELETE existing_has_camera_setting_control
                            DELETE existing_has_metadata_version
                            DELETE existing_has_metadata_source
                            DELETE existing_has_file_format
                            DELETE existing_has_status
                            DELETE existing_child_of
                            DELETE existing_funds
                            MERGE (asset)-[:HAS]->(new_status)
                            MERGE (user:User{user_id: $user, name: $user})
                            MERGE (update_event:Event{timestamp: $updated_date, event:'UPDATE_ASSET_METADATA', name: 'UPDATE_ASSET_METADATA'})
                            MERGE (update_event)-[uw:USED]->(workstation)
                            MERGE (update_event)-[up:USED]->(pipeline)
                            MERGE (update_event)-[pb:INITIATED_BY]->(user)
                            MERGE (asset)-[ca:CHANGED_BY]->(update_event)
                            SET asset.status = $status
                            , asset.tags = $tags
                            , asset.subject = $subject
                            , asset.payload_type = $payload_type
                            , asset.restricted_access = $restricted_access
                            , asset.date_asset_finalised = $date_asset_finalised
                            , asset.parent_id = $parent_id
                            , asset.asset_locked = $asset_locked
                            , asset.internal_status = $internal_status
                            , asset.date_asset_taken = $date_asset_taken
                            , asset.date_metadata_ingested = $date_metadata_ingested
                            , asset.make_public = $make_public
                            , asset.push_to_specify = $push_to_specify
                """);
        if (asset.date_asset_taken != null) {
            agBuilder.add("date_asset_taken", asset.date_asset_taken.toEpochMilli());
        } else {
            agBuilder.add("date_asset_taken", (String) null);
        }
        if (asset.date_metadata_ingested != null) {
            agBuilder.add("date_metadata_ingested", asset.date_metadata_ingested.toEpochMilli());
        } else {
            agBuilder.add("date_metadata_ingested", (String) null);
        }
        if (asset.date_asset_finalised != null) {
            agBuilder.add("date_asset_finalised", asset.date_asset_finalised.toEpochMilli());
        } else {
            agBuilder.add("date_asset_finalised", (String) null);
        }
        if (!Strings.isNullOrEmpty(asset.digitiser)) {
            sb.append("""
                            MERGE (new_digitiser:Digitiser{name: $new_digitiser})
                            MERGE (asset)<-[new_digitised:DIGITISED]-(new_digitiser)
                    """);
            agBuilder.add("new_digitiser", asset.digitiser);
        }
        // Nullable relations
        if (!Strings.isNullOrEmpty(asset.subject)) {
            sb.append("""
                            MERGE (new_subject:Subject{name: $subject})
                            MERGE (asset)-[new_asset_has_subject:HAS]->(new_subject)
                    """);
            agBuilder.add("subject", asset.subject);

        }
        if (!Strings.isNullOrEmpty(asset.camera_setting_control)) {
            sb.append("""
                            MERGE (new_camera_setting_control:Camera_setting_control{name: $camera_setting_control})
                            MERGE (asset)-[new_has_camera_setting_control:HAS]->(new_camera_setting_control)
                    """);
            agBuilder.add("camera_setting_control", asset.camera_setting_control);
        }
        if (!Strings.isNullOrEmpty(asset.metadata_version)) {
            sb.append("""
                            MERGE (new_metadata_version:Metadata_version{name: $new_metadata_version})
                            MERGE (asset)-[new_has_metadata_version:HAS]->(new_metadata_version)
                    """);
            agBuilder.add("new_metadata_version", asset.metadata_version);
        }
        if (!Strings.isNullOrEmpty(asset.metadata_source)) {
            sb.append("""
                            MERGE (new_metadata_source:Metadata_source{name: $metadata_source})
                            MERGE (asset)-[new_has_metadata_source:HAS]->(new_metadata_source)
                    """);
            agBuilder.add("metadata_source", asset.metadata_source);
        }

        if (asset.payload_type != null) {
            sb.append("""
                            MERGE (new_payload_type:Payload_type{name: $payload_type})
                            MERGE (asset)-[new_has_payload_type:HAS]->(new_payload_type)
                    """);
            agBuilder.add("payload_type", asset.payload_type);
        }
        // List objects
        for(int i = 0 ; i < asset.funding.size(); i++) {
            sb.append("          MERGE (new_funding")
                    .append(i)
                    .append(":Funding_entity{name: $new_funding")
                    .append(i)
                    .append("})\n        MERGE (asset)<-[:FUNDS]-(new_funding")
                    .append(i).append(")");
            agBuilder.add("new_funding" + i, asset.funding.get(i).name());
        }
        for(int i = 0 ; i < asset.issues.size(); i++) {
            sb.append("      MERGE (new_issues")
                    .append(i)
                    .append(":Issue{name: $new_issues")
                    .append(i)
                    .append("})\n        MERGE (asset)-[:HAS]->(new_issues")
                    .append(i).append(")");
            agBuilder.add("new_issues" + i, asset.issues.get(i).issue());
        }
        for(int i = 0 ; i < asset.file_formats.size(); i++) {
            sb.append("      MERGE (new_file_format")
                    .append(i)
                    .append(":File_format{name: $new_file_format")
                    .append(i)
                    .append("})\n        MERGE (asset)-[:HAS]->(new_file_format")
                    .append(i).append(")");
            agBuilder.add("new_file_format" + i, asset.file_formats.get(i));
        }
        for(int i = 0 ; i < asset.complete_digitiser_list.size(); i++) {
            sb.append("      MERGE (digitiser")
                    .append(i)
                    .append(":Digitiser{name: $digitiser")
                    .append(i)
                    .append("})\n        MERGE (asset)<-[:WORKED_ON]-(digitiser")
                    .append(i).append(")");
            agBuilder.add("digitiser" + i, asset.complete_digitiser_list.get(i));
        }
        sb.append("""
                \n$$
                        , #params) as (a agtype);
                """);
        return sb.toString();
    }
    default Asset update_asset_internal(Asset asset) {

        AgtypeListBuilder agtypeListBuilder = new AgtypeListBuilder();
        asset.file_formats.forEach(agtypeListBuilder::add);
        AgtypeMapBuilder tags = new AgtypeMapBuilder();
        asset.tags.entrySet().forEach(tag -> tags.add(tag.getKey(), tag.getValue())); //(tag -> tags.add(tag));
        AgtypeListBuilder restrictedAcces = new AgtypeListBuilder();
        asset.restricted_access.forEach(role -> restrictedAcces.add(role.name()));
        AgtypeMapBuilder builder = new AgtypeMapBuilder()
                .add("collection_name", asset.collection)
                .add("workstation_name", asset.workstation)
                .add("pipeline_name", asset.pipeline)
                .add("asset_guid", asset.asset_guid)
                .add("status", asset.status)
//                        .add("funding", asset.funding)
                .add("subject", asset.subject)
                .add("payload_type", asset.payload_type)
                .add("file_formats", agtypeListBuilder.build())
                .add("updated_date", Instant.now().toEpochMilli())
                .add("internal_status", asset.internal_status.name())
                .add("parent_id", asset.parent_guid)
                .add("user", asset.updateUser)
                .add("tags", tags.build())
                .add("asset_locked", asset.asset_locked)
                .add("restricted_access", restrictedAcces.build())
                .add("make_public", asset.make_public)
                .add("push_to_specify", asset.push_to_specify);

        String sql = buildUpdateSQL(asset, builder);
        logger.info(sql);
        try {
            withHandle(handle -> {
                Agtype agtype = AgtypeFactory.create(builder.build());
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

    @Transaction
    default void deleteAsset(String assetGuid) {
        boilerplate();
        // Deletes Asset and removes connections to Specimens and Events.
        // The query then removes orphaned Specimens and Events (Specimens and Events not connected to any Asset).
        internal_deleteAsset(assetGuid);
    }

    default void internal_deleteAsset(String assetGuid) {
        // Deletes Asset
        String sqlAsset = """
                SELECT * FROM ag_catalog.cypher('dassco'
                , $$
                    MATCH (a:Asset {name: $asset_guid})
                    DETACH DELETE a
                $$
                , #params) as (a agtype);
                """;
        // Deletes orphaned Specimens:
        String sqlSpecimen = """
                SELECT * FROM ag_catalog.cypher('dassco'
                , $$
                    MATCH (s:Specimen)
                    WHERE NOT EXISTS((s)-[:USED_BY]-())
                    DETACH DELETE s
                $$
                ) as (s agtype);
                """;
        // Deletes orphaned Events:
        String sqlEvent = """
                SELECT * FROM ag_catalog.cypher('dassco'
                , $$
                    MATCH (e:Event)
                    WHERE NOT EXISTS((e)-[:CHANGED_BY]-())
                    DETACH DELETE e
                $$
                ) as (e agtype);
                """;

        try {
            withHandle(handle -> {
                AgtypeMapBuilder builder = new AgtypeMapBuilder()
                        .add("asset_guid", assetGuid);
                Agtype agtype = AgtypeFactory.create(builder.build());
                handle.createUpdate(sqlAsset)
                        .bind("params", agtype)
                        .execute();
                handle.createUpdate(sqlSpecimen)
                        .execute();
                handle.createUpdate(sqlEvent)
                        .execute();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transaction
    default void bulkUpdateAssets(String sql, AgtypeMapBuilder builder) {
        try {
            withHandle(handle -> {
                Agtype agtype = AgtypeFactory.create(builder.build());
                handle.createUpdate(sql)
                        .bind("params", agtype)
                        .execute();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transaction
    default void setTags(Asset asset) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (a:Asset {name: $asset_guid})
                            
                            SET a.tags = $tags
                        $$
                        , #params) as (a agtype);
                        """;

        try {
            withHandle(handle -> {
                AgtypeMapBuilder tags = new AgtypeMapBuilder();
                asset.tags.entrySet().forEach(tag -> tags.add(tag.getKey(), tag.getValue())); //(tag -> tags.add(tag));
                AgtypeMapBuilder builder = new AgtypeMapBuilder()
                        .add("asset_guid", asset.asset_guid)
                        .add("tags", tags.build());
                Agtype agtype = AgtypeFactory.create(builder.build());
                handle.createUpdate(sql)
                        .bind("params", agtype)
                        .execute();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transaction
    default void setEvent(String user, Event event, Asset asset) {
        boilerplate();
        internal_setEvent(user, event, asset);
    }

    default void internal_setEvent(String user, Event event, Asset asset) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (a:Asset {name: $asset_guid})
                            """;
        if (event.pipeline != null) {
            sql += "MATCH (p:Pipeline {name: $pipeline_name}) ";
        }
        if (event.workstation != null) {
            sql += "MATCH (w:Workstation {name: $workstation_name}) ";
        }
        if (event.user != null) {

            sql += "MERGE (u:User{user_id: $user, name: $user}) ";
        }
        sql +=
                """
                        MERGE (e:Event{timestamp: $updated_date, event: $event, name: $event})
                        MERGE (a)-[ca:CHANGED_BY]->(e)
                        """;
        if (event.user != null) {
            sql += " MERGE (e)-[pb:INITIATED_BY]->(u) ";
        }
        if (event.pipeline != null) {
            sql += " MERGE (e)-[pu:USED]->(p) ";
        }
        if (event.workstation != null) {
            sql += " MERGE (e)-[wu:USED]->(w) ";
        }
        sql +=
                """
                        $$
                        , #params) as (a agtype);
                        """;

        try {
            String finalSql = sql;
            withHandle(handle -> {
                AgtypeMapBuilder builder = new AgtypeMapBuilder()
                        .add("asset_guid", asset.asset_guid)
//                        .add("user", user)
                        .add("event", event.event.name())
                        .add("updated_date", event.timeStamp.toEpochMilli());
                if (event.user != null) {
                    builder.add("user", event.user);
                }
                if (event.workstation != null) {
                    builder.add("workstation_name", event.workstation);
                }
                if (event.pipeline != null) {
                    builder.add("pipeline_name", event.pipeline);
                }

                Agtype agtype = AgtypeFactory.create(builder.build());
                handle.createUpdate(finalSql)
                        .bind("params", agtype)
                        .execute();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }





}
