package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.DasscoEvent;
import dk.northtech.dasscoassetservice.domain.Event;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
        Optional<Asset> asset = readAssetInternal(assetId);
        if(asset.isEmpty()) {
            return asset;
        }
        Asset asset1 = asset.get();
        List<Event> events = readEvents_internal(assetId);

        for(Event event : events) {
            if(DasscoEvent.AUDIT_ASSET.equals(event.event)) {
                asset1.audited = true;
            } else if(DasscoEvent.UPDATE_ASSET.equals(event.event) && asset1.last_updated_date == null) {
                asset1.last_updated_date = event.timeStamp;
            } else if(DasscoEvent.CREATE_ASSET.equals(event.event) && asset1.last_updated_date == null) {
                asset1.last_updated_date = event.timeStamp;
            } else if(DasscoEvent.DELETE_ASSET.equals(event.event)) {
                asset1.asset_deleted_date = event.timeStamp;
            }
        }
        return Optional.of(asset1);
    }

    @Transaction
    default Asset updateAsset(Asset asset) {
        boilerplate();
        update_asset_internal(asset);
        connectParentChild(asset.parent_guid, asset.guid);
        createSpecimenRepository().persistSpecimens(asset);
        return asset;
    }

    @Transaction
    default Asset updateAssetNoEvent(Asset asset) {
        boilerplate();
        updateAssetNoEventInternal(asset);
        return asset;
    }

    default Optional<Asset> readAssetInternal(String assetGuid) {
        String sql = """
                SELECT * FROM ag_catalog.cypher(
                'dassco'
                    , $$
                         MATCH (a:Asset{name: $guid})
                         MATCH (c:Collection)<-[:IS_PART_OF]-(a)
                         MATCH (e:Event{event:'CREATE_ASSET'})<-[:CHANGED_BY]-(a)
                         MATCH (u:User)<-[:INITIATED_BY]-(e)
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
                         , a.asset_locked
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
                         , u.name
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
                    , asset_locked agtype
                    , parent_guid agtype
                    , restricted_access agtype
                    , tags agtype
                    , specimen_barcodes agtype
                    , institution_name agtype
                    , collection_name agtype
                    , pipeline_name agtype
                    , workstation_name agtype
                    , creation_date agtype
                    , pushed_to_specify_date agtype
                    , user_name agtype);
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
                            MATCH (e:Event)<-[:CHANGED_BY]-(a)
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
                    .add("guid", guid)
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
                                , asset_locked: $asset_locked
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
                AgtypeMapBuilder agBuilder = new AgtypeMapBuilder()
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
                        .add("created_date", asset.created_date.toEpochMilli())
                        .add("internal_status", asset.internal_status.name())
                        .add("parent_id",asset.parent_guid)
                        .add("user", asset.digitizer)
                        .add("tags",tags.build())
                        .add("restricted_access", restrictedAcces.build())
                        .add("asset_locked", asset.asset_locked);

                if(asset.asset_taken_date != null) {
                    agBuilder.add("asset_taken_date", asset.asset_taken_date.toEpochMilli());
                } else {
                    agBuilder.add("asset_taken_date", (String) null);
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
                            MATCH (a:Asset {name: $guid})
                            SET a.asset_locked = $asset_locked
                            , a.internal_status = $internal_status
                        $$
                        , #params) as (a agtype);
                        """;
        try {
            withHandle(handle -> {
                AgtypeMapBuilder builder = new AgtypeMapBuilder()
                        .add("guid", asset.guid)
                        .add("internal_status", asset.internal_status.name())
                        .add("asset_locked", asset.asset_locked);
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
                            MATCH (a:Asset {name: $guid})
                            OPTIONAL MATCH (a)-[co:CHILD_OF]-(parent:Asset)
                            DELETE co
                            MERGE (u:User{user_id: $user, name: $user})
                            MERGE (e:Event{timestamp: $updated_date, event:'UPDATE_ASSET', name: 'UPDATE_ASSET'})
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
                            , a.pushed_to_specify_date = $pushed_to_specify_date
                            , a.parent_id = $parent_id
                            , a.asset_locked = $asset_locked
                            , a.internal_status = $internal_status
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
                        .add("guid", asset.guid)
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
                if(asset.pushed_to_specify_date != null) {
                    builder.add("pushed_to_specify_date", asset.pushed_to_specify_date.toEpochMilli());
                } else {
                    builder.addNull("pushed_to_specify_date");
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
    default void setEvent(String user, DasscoEvent event, Asset asset) {
        boilerplate();
        internal_setEvent(user, event, asset);
    };

    default void internal_setEvent(String user, DasscoEvent dasscoEvent, Asset asset) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (a:Asset {name: $guid})
                            MERGE (u:User{user_id: $user, name: $user})
                            MERGE (e:Event{timestamp: $updated_date, event: $event, name: $event})
                            MERGE (e)-[pb:INITIATED_BY]->(u)
                            MERGE (a)-[ca:CHANGED_BY]-(e)
                        $$
                        , #params) as (a agtype);
                        """;
        try {
            withHandle(handle -> {
                AgtypeMapBuilder builder = new AgtypeMapBuilder()
                        .add("guid", asset.guid)
                        .add("user", user)
                        .add("event", dasscoEvent.name())
                        .add("updated_date", Instant.now().toEpochMilli());
                Agtype agtype = AgtypeFactory.create(builder.build());
                handle.createUpdate(sql)
                        .bind("params", agtype)
                        .execute();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };
}
