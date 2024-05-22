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

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


//@Repository
public interface AssetRepository extends SqlObject {
//    private Jdbi jdbi;
//    private DataSource dataSource;

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
    default Asset createAsset(Asset asset) {
        boilerplate();
        persistAsset(asset);
        createSpecimenRepository().persistSpecimens(asset, new ArrayList<>());
        connectParentChild(asset.parent_guid, asset.asset_guid);
        return asset;
    }

    @Transaction
    default Optional<Asset> readAsset(String assetId) {
        boilerplate();
        Optional<Asset> asset = readAssetInternal(assetId);
        if (asset.isEmpty()) {
            return asset;
        }
        Asset asset1 = asset.get();
        List<Event> events = readEvents_internal(assetId);

        for (Event event : events) {
            if (DasscoEvent.AUDIT_ASSET.equals(event.event)) {
                asset1.audited = true;
            } else if (DasscoEvent.UPDATE_ASSET_METADATA.equals(event.event) && asset1.date_metadata_updated == null) {
                asset1.date_metadata_updated = event.timeStamp;
            } else if (DasscoEvent.CREATE_ASSET_METADATA.equals(event.event) && asset1.date_metadata_updated == null) {
                asset1.date_metadata_updated = event.timeStamp;
            } else if (DasscoEvent.DELETE_ASSET_METADATA.equals(event.event)) {
                asset1.date_asset_deleted = event.timeStamp;
            }
        }
        asset1.events = events;
        return Optional.of(asset1);
    }

    @Transaction
    default List<Asset> readMultipleAssets(List<String> assets){
        boilerplate();
        return readMultipleAssetsInternal(assets);
    }

    @Transaction
    default Asset updateAsset(Asset asset, List<Specimen> specimenToDetach) {
        boilerplate();
        update_asset_internal(asset);
        connectParentChild(asset.parent_guid, asset.asset_guid);
        createSpecimenRepository().persistSpecimens(asset, specimenToDetach);
        return asset;
    }

    @Transaction
    default void bulkUpdate(String sql, AgtypeMapBuilder builder, Asset updatedAsset, Event event, Map<Asset, List<Specimen>> assetAndSpecimens){
        boilerplate();
        // Update asset metadata:
        bulkUpdateAssets(sql, builder);
        // Add Event to every asset:
        // TODO: Could not make the bulk update create an event as well. It would either create multiple events
        // TODO: or, when trying to UNWIND the assets to merge the new event with them, give errors with the compatibility with agtype vertex.

        for (Map.Entry<Asset, List<Specimen>> entry : assetAndSpecimens.entrySet()) {
            Asset asset = entry.getKey();
            List<Specimen> specimenList = entry.getValue();
            // Set event (individual calls)
            setEvent(updatedAsset.updateUser, event, asset);
            // Connect parent and child (individual calls)
            connectParentChild(updatedAsset.parent_guid, asset.asset_guid);
            // Detach and persist the specimens (individual calls).
            createSpecimenRepository().persistSpecimens(asset, specimenList);
        }
    }

    @Transaction
    default Asset updateAssetNoEvent(Asset asset) {
        boilerplate();
        updateAssetNoEventInternal(asset);
        return asset;
    }

    @Transaction
    default Asset updateAssetAndEvent(Asset asset, Event event) {
        boilerplate();
        updateAssetNoEventInternal(asset);
        setEvent(event.user, event, asset);
        return asset;
    }

    default Optional<Asset> readAssetInternal(String assetGuid) {
        String sql = """
                SELECT * FROM ag_catalog.cypher(
                'dassco'
                    , $$
                         MATCH (a:Asset{name: $asset_guid})
                         MATCH (c:Collection)<-[:IS_PART_OF]-(a)
                         MATCH (e:Event{event:'CREATE_ASSET_METADATA'})<-[:CHANGED_BY]-(a)
                         MATCH (u:User)<-[:INITIATED_BY]-(e)
                         MATCH (p:Pipeline)<-[:USED]-(e)
                         MATCH (w:Workstation)<-[:USED]-(e)
                         MATCH (i:Institution)<-[:BELONGS_TO]-(a)
                         OPTIONAL MATCH (s:Specimen)-[sss:USED_BY]->(:Asset{name: $asset_guid})
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
                         , pa.asset_guid 
                         , a.restricted_access
                         , a.tags
                         , a.error_message
                         , a.error_timestamp
                         , collect(s)
                         , i.name
                         , c.name
                         , p.name
                         , w.name
                         , e.timestamp
                         , a.date_asset_finalised
                         , u.name
                         , a.date_metadata_taken
                         , a.date_asset_taken
                      $$
                    , #params)
                    as (asset_guid agtype
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
                    , date_asset_taken agtype);
                  """;
        return withHandle(handle -> {
            AgtypeMap agParams = new AgtypeMapBuilder()
                    .add("asset_guid", assetGuid)
                    .add("asset_guid", assetGuid)//TODO see if we can delete this
                    .build();
            Agtype agtype = AgtypeFactory.create(agParams);
            return handle.createQuery(sql)
                    .bind("params", agtype)
                    .map(new AssetMapper())
                    .findOne();
        });
    }


    default List<Asset> readMultipleAssetsInternal(List<String> assets){
        String assetListAsString = assets.stream()
                .map(asset -> "'" + asset + "'")
                .collect(Collectors.joining(", "));

        String sql = """
                SELECT * FROM ag_catalog.cypher(
                'dassco'
                    , $$
                         MATCH (a:Asset)
                         WHERE a.asset_guid IN [%s]
                         MATCH (c:Collection)<-[:IS_PART_OF]-(a)
                         MATCH (e:Event{event:'CREATE_ASSET_METADATA'})<-[:CHANGED_BY]-(a)
                         MATCH (u:User)<-[:INITIATED_BY]-(e)
                         MATCH (p:Pipeline)<-[:USED]-(e)
                         MATCH (w:Workstation)<-[:USED]-(e)
                         MATCH (i:Institution)<-[:BELONGS_TO]-(a)
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
                         , pa.asset_guid
                         , a.restricted_access
                         , a.tags
                         , a.error_message
                         , a.error_timestamp
                         , collect(s)
                         , i.name
                         , c.name
                         , p.name
                         , w.name
                         , e.timestamp
                         , a.date_asset_finalised
                         , u.name
                         , a.date_metadata_taken
                         , a.date_asset_taken
                      $$
                    )
                    as (asset_guid agtype
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
                    , date_asset_taken agtype);
                  """.formatted(assetListAsString);

        return withHandle(handle -> handle.createQuery(sql)
                .map(new AssetMapper())
                .list());
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

    default Asset persistAsset(Asset asset) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (i:Institution {name: $institution_name})
                            MATCH (c:Collection {name: $collection_name})
                            MATCH (w:Workstation {name: $workstation_name})
                            MATCH (p:Pipeline {name: $pipeline_name})
                            MERGE (a:Asset {name: $asset_guid
                                , asset_pid: $asset_pid
                                , asset_guid: $asset_guid
                                , status: $status
                                , funding: $funding
                                , subject: $subject
                                , payload_type: $payload_type
                                , file_formats: $file_formats
                                , asset_taken_date: $asset_taken_date
                                , internal_status: $internal_status
                                , restricted_access: $restricted_access
                                , tags: $tags
                                , asset_locked: $asset_locked
                                , date_metadata_taken: $date_metadata_taken
                            })
                            MERGE (u:User{user_id: $user, name: $user})
                            MERGE (e:Event{timestamp: $created_date, event:'CREATE_ASSET_METADATA', name: 'CREATE_ASSET_METADATA'})
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
                AgtypeMapBuilder agBuilder = new AgtypeMapBuilder()
                        .add("institution_name", asset.institution)
                        .add("collection_name", asset.collection)
                        .add("workstation_name", asset.workstation)
                        .add("pipeline_name", asset.pipeline)
                        .add("asset_pid", asset.asset_pid)
                        .add("asset_guid", asset.asset_guid)
                        .add("status", asset.status.name())
                        .add("funding", asset.funding)
                        .add("subject", asset.subject)
                        .add("payload_type", asset.payload_type)
                        .add("file_formats", agtypeListBuilder.build())
                        .add("created_date", asset.created_date.toEpochMilli())
                        .add("internal_status", asset.internal_status.name())
                        .add("parent_id", asset.parent_guid)
                        .add("user", asset.digitiser)
                        .add("tags", tags.build())
                        .add("restricted_access", restrictedAcces.build())
                        .add("asset_locked", asset.asset_locked);

                if (asset.date_asset_taken != null) {
                    agBuilder.add("date_asset_taken", asset.date_asset_taken.toEpochMilli());
                } else {
                    agBuilder.add("date_asset_taken", (String) null);
                }
                if (asset.date_metadata_taken != null) {
                    agBuilder.add("date_metadata_taken", asset.date_metadata_taken.toEpochMilli());
                } else {
                    agBuilder.add("date_metadata_taken", (String) null);
                }
                AgtypeMap parms = agBuilder.build();
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

    default Asset updateAssetNoEventInternal(Asset asset) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (a:Asset {name: $asset_guid})
                            SET a.asset_locked = $asset_locked
                            , a.internal_status = $internal_status
                            , a.error_message = $error_message
                            , a.error_timestamp = $error_timestamp
                        $$
                        , #params) as (a agtype);
                        """;
        try {
            withHandle(handle -> {
                AgtypeMapBuilder builder = new AgtypeMapBuilder()
                        .add("asset_guid", asset.asset_guid)
                        .add("internal_status", asset.internal_status.name())
                        .add("error_message", asset.error_message)
                        .add("asset_locked", asset.asset_locked);
                if (asset.error_timestamp != null) {
                    builder.add("error_timestamp", asset.error_timestamp.toEpochMilli());
                } else {
                    builder.add("error_timestamp", (String) null);
                }
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

    default Asset update_asset_internal(Asset asset) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (c:Collection {name: $collection_name})
                            MATCH (w:Workstation {name: $workstation_name})
                            MATCH (p:Pipeline {name: $pipeline_name})                        
                            MATCH (a:Asset {name: $asset_guid})
                            OPTIONAL MATCH (a)-[co:CHILD_OF]-(parent:Asset)
                            DELETE co
                            MERGE (u:User{user_id: $user, name: $user})
                            MERGE (e:Event{timestamp: $updated_date, event:'UPDATE_ASSET_METADATA', name: 'UPDATE_ASSET_METADATA'})
                            MERGE (e)-[uw:USED]->(w)
                            MERGE (e)-[up:USED]->(p)
                            MERGE (e)-[pb:INITIATED_BY]->(u)
                            MERGE (a)-[ca:CHANGED_BY]-(e)
                            SET a.status = $status
                            , a.tags = $tags
                            , a.funding = $funding
                            , a.subject = $subject
                            , a.payload_type = $payload_type
                            , a.file_formats = $file_formats
                            , a.restricted_access = $restricted_access
                            , a.date_asset_finalised = $date_asset_finalised
                            , a.parent_id = $parent_id
                            , a.asset_locked = $asset_locked
                            , a.internal_status = $internal_status
                            , a.date_metadata_taken = $date_metadata_taken
                            , a.date_asset_taken = $date_asset_taken
                            , a.digitiser = $digitiser
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
                AgtypeMapBuilder builder = new AgtypeMapBuilder()
                        .add("collection_name", asset.collection)
                        .add("workstation_name", asset.workstation)
                        .add("pipeline_name", asset.pipeline)
                        .add("asset_guid", asset.asset_guid)
                        .add("status", asset.status.name())
                        .add("funding", asset.funding)
                        .add("subject", asset.subject)
                        .add("payload_type", asset.payload_type)
                        .add("file_formats", agtypeListBuilder.build())
                        .add("updated_date", Instant.now().toEpochMilli())
                        .add("internal_status", asset.internal_status.name())
                        .add("parent_id", asset.parent_guid)
                        .add("user", asset.updateUser)
                        .add("tags", tags.build())
                        .add("asset_locked", asset.asset_locked)
                        .add("restricted_access", restrictedAcces.build());
                if (asset.date_metadata_taken != null) {
                    builder.add("date_metadata_taken", asset.date_metadata_taken.toEpochMilli());
                } else {
                    builder.addNull("date_metadata_taken");
                }
                if (asset.date_asset_finalised != null) {
                    builder.add("date_asset_finalised", asset.date_asset_finalised.toEpochMilli());
                } else {
                    builder.addNull("date_asset_finalised");
                }
                if (asset.date_asset_taken != null) {
                    builder.add("date_asset_taken", asset.date_asset_finalised.toEpochMilli());
                } else {
                    builder.addNull("date_asset_taken");
                }
                if (asset.digitiser != null) {
                    builder.add("digitiser", asset.digitiser);
                } else {
                    builder.addNull("digitiser");
                }
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
    default void bulkUpdateAssets(String sql, AgtypeMapBuilder builder){
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
                        MERGE (a)-[ca:CHANGED_BY]-(e)
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
