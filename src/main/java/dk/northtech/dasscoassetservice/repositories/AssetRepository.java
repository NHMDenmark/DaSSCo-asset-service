package dk.northtech.dasscoassetservice.repositories;

import com.google.gson.Gson;
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
import java.sql.Timestamp;
import java.util.*;


//@Repository
public interface AssetRepository extends SqlObject {
    //    private Jdbi jdbi;
//    private DataSource dataSource;
    static final Logger logger = LoggerFactory.getLogger(AssetRepository.class);

    static String INSERT_BASE_ASSET =
            """
            INSERT INTO public.asset(
                     asset_guid
                    , asset_pid
                    , asset_locked
                    , subject
                    , collection_id
                    , digitiser_id
                    , file_format
                    , payload_type
                    , status
                    , tags
                    , workstation_id
                    , internal_status
                    , make_public
                    , metadata_source
                    , push_to_specify
                    , metadata_version
                    , camera_setting_control
                    , date_asset_taken
                    , date_asset_finalised
                    , initial_metadata_recorded_by
                    , date_metadata_ingested
                  ) VALUES (
                    :assetGuid
                    , :asset_pid
                    , :assetLocked
                    , :subject
                    , :collectionId
                    , :digitiserId
                    , :fileFormat
                    , :payloadType
                    , :status
                    , :tags::jsonb
                    , :workstationId
                    , :internalStatus
                    , :makePublic
                    , :metadataSource
                    , :pushToSpecify
                    , :metadataVersion
                    , :cameraSettingControl
                    , :date_asset_taken
                    , :dateAssetFinalised
                    , :initialMetadataRecordedBy
                    , :dateMetadataIngested
                  );
    """;

    default void insertBaseAsset(Asset asset) {
        withHandle(handle -> {
            handle.createUpdate(INSERT_BASE_ASSET)
                    .bind("assetGuid", asset.asset_guid)
                    .bind("asset_pid", asset.asset_pid)
                    .bind("assetLocked", asset.asset_locked)
                    .bind("subject", asset.subject != null? asset.subject.toLowerCase():null)
                    .bind("collectionId", asset.collection_id)
                    .bind("digitiserId", asset.digitiser_id)
                    .bind("fileFormat", asset.file_format)
                    .bind("multiSpecimen", asset.multi_specimen)
                    .bind("payloadType", asset.payload_type)
                    .bind("status", asset.status)
                    .bind("tags", new Gson().toJson( asset.tags)) // Assuming 'tags' is a Map or List of JSON-compatible types
                    .bind("workstationId", asset.workstation_id)
                    .bind("internalStatus", asset.internal_status)
                    .bind("makePublic", asset.make_public)
                    .bind("metadataSource", asset.metadata_source)
                    .bind("pushToSpecify", asset.push_to_specify)
                    .bind("metadataVersion", asset.metadata_version)
                    .bind("cameraSettingControl", asset.camera_setting_control)
                    .bind("date_asset_taken", asset.date_asset_taken != null ? Timestamp.from(asset.date_asset_taken):null)
                    .bind("dateAssetFinalised", asset.date_asset_finalised != null ?Timestamp.from(asset.date_asset_finalised):null)
                    .bind("initialMetadataRecordedBy", asset.initial_metadata_recorded_by)
                    .bind("dateMetadataIngested", asset.date_metadata_ingested != null ? Timestamp.from(asset.date_metadata_ingested): null)
                    .execute();
            return handle;
        });
    };
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

//    @Transaction
//    default Asset createAsset(Asset asset) {
//        boilerplate();
//        persistAssetNew(asset);
//        createSpecimenRepository().persistSpecimens(asset, new ArrayList<>());
//        connectParentChild(asset.parent_guid, asset.asset_guid);
//        return asset;
//    }

    String READ_ASSET = """
            SELECT asset.*
                , collection.collection_name
                , collection.institution_name
                , dassco_user.username AS digitiser
                , workstation.workstation_name 
            FROM asset
            LEFT JOIN collection USING(collection_id)
            LEFT JOIN workstation USING(workstation_id)  
            LEFT JOIN dassco_user ON dassco_user.dassco_user_id = asset.digitiser_id
            """;

    @Transaction
    default Optional<Asset> readAsset(String assetId) {

        boilerplate();
        Optional<Asset> asset = readAssetInternalNew(assetId);
        if (asset.isEmpty()) {
            return asset;
        }
        Asset asset1 = asset.get();
        List<Event> events = readEvents_internal(assetId);

        for (Event event : events) {
            if (DasscoEvent.AUDIT_ASSET.equals(event.event)) {
                asset1.audited = true;
            } else if (DasscoEvent.BULK_UPDATE_ASSET_METADATA.equals(event.event) && asset1.date_metadata_updated == null) {
                asset1.date_metadata_updated = event.timestamp;
            } else if (DasscoEvent.UPDATE_ASSET_METADATA.equals(event.event) && asset1.date_metadata_updated == null) {
                asset1.date_metadata_updated = event.timestamp;
            } else if (DasscoEvent.CREATE_ASSET_METADATA.equals(event.event) && asset1.date_metadata_updated == null) {
                asset1.date_metadata_updated = event.timestamp;
            } else if (DasscoEvent.DELETE_ASSET_METADATA.equals(event.event)) {
                asset1.date_asset_deleted = event.timestamp;
            }
        }
        asset1.events = events;
        return Optional.of(asset1);
    }


    @Transaction
    default Asset updateAsset(Asset asset, List<Specimen> specimenToDetach) {
        boilerplate();
        update_asset_internal(asset);
        connectParentChild(asset.parent_guid, asset.asset_guid);
//        createSpecimenRepository().persistSpecimens(asset, specimenToDetach);
        return asset;
    }


    @Transaction
    default Asset updateAssetNoEvent(Asset asset) {
        updateAssetNoEventInternal(asset);
        return asset;
    }

    @Transaction
    default Asset updateAssetStatus(Asset asset) {
        updateAssetNoEventInternal(asset);
//        setEvent(event.user, event, asset);
        return asset;
    }

    default Optional<Asset> readAssetInternalNew(String assetGuid) {
        String sql = READ_ASSET + " WHERE asset_guid = :asset_guid";
        return withHandle(handle -> {
            return handle.createQuery(sql)
                    .bind("asset_guid", assetGuid)
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

            events.sort(Collections.reverseOrder(Comparator.comparing(event -> event.timestamp)));
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

    static String buildCreateSQL(Asset asset, AgtypeMapBuilder agBuilder) {
        StringBuilder sb = new StringBuilder("""
                SELECT * FROM ag_catalog.cypher('dassco'
                    , $$
                        MATCH (i:Institution {name: $institution_name})
                        MATCH (c:Collection {name: $collection_name})
                        MATCH (w:Workstation {name: $workstation_name})
                        MATCH (p:Pipeline {name: $pipeline_name})
                        MATCH (ist:Internal_status{name: $internal_status})
                        MATCH (s:Status{name: $status})
                        MERGE (a:Asset {name: $asset_guid
                            , asset_pid: $asset_pid
                            , asset_guid: $asset_guid
                            , date_asset_taken: $date_asset_taken
                            , tags: $tags
                            , asset_locked: $asset_locked
                            , date_metadata_ingested: $date_metadata_ingested
                            , date_asset_finalised: $date_asset_finalised
                            , make_public: $make_public
                            , push_to_specify: $push_to_specify
                        })
                        MERGE (a)-[ahs:HAS]->(s)
                        MERGE (a)-[ahi:HAS]->(ist)
                """);

        if (asset.date_asset_taken != null) {
            agBuilder.add("date_asset_taken", asset.date_asset_taken.toEpochMilli());
        } else {
            agBuilder.add("date_asset_taken", (String) null);
        }
        if (asset.date_metadata_ingested != null) {
            System.out.println("D8 is not null");
            agBuilder.add("date_metadata_ingested", asset.date_metadata_ingested.toEpochMilli());
        } else {
            agBuilder.add("date_metadata_ingested", (String) null);
        }
        if (asset.date_asset_finalised != null) {
            agBuilder.add("date_asset_finalised", asset.date_asset_finalised.toEpochMilli());
        } else {
            agBuilder.add("date_asset_finalised", (String) null);
        }
        if (!Strings.isNullOrEmpty(asset.subject)) {
            sb.append("""
                            MERGE (sb:Subject{name: $subject})
                            MERGE (a)-[aha:HAS]->(sb)
                    """);
            agBuilder.add("subject", asset.subject);

        }
        if (!Strings.isNullOrEmpty(asset.camera_setting_control)) {
            sb.append("""
                            MERGE (csc:Camera_setting_control{name: $camera_setting_control})
                            MERGE (a)-[:HAS]->(csc)
                    """);
            agBuilder.add("camera_setting_control", asset.camera_setting_control);
        }
        if (!Strings.isNullOrEmpty(asset.metadata_version)) {
            sb.append("""
                            MERGE (mv:Metadata_version{name: $metadata_version})
                            MERGE (a)-[:HAS]->(mv)
                    """);
            agBuilder.add("metadata_version", asset.metadata_version);
        }
        if (!Strings.isNullOrEmpty(asset.metadata_source)) {
            sb.append("""
                            MERGE (mds:Metadata_source{name: $metadata_source})
                            MERGE (a)-[:HAS]->(mds)
                    """);
            agBuilder.add("metadata_source", asset.metadata_source);
        }

        if (asset.payload_type != null) {
            sb.append("""
                            MERGE (pt:Payload_type{name: $payload_type})
                            MERGE (a)-[ahp:HAS]->(pt)
                    """);
            agBuilder.add("payload_type", asset.payload_type);
        }
        if (asset.digitiser != null) {
            sb.append("""
                            MERGE (digitiser:Digitiser{name: $digitiser})
                            MERGE (a)<-[:DIGITISED]-(digitiser)
                    """);
            agBuilder.add("digitiser", asset.digitiser);
        }

        for(int i = 0 ; i < asset.funding.size(); i++) {
            sb.append("MERGE (funding")
                    .append(i)
                    .append(":Funding_entity{name: $funding")
                    .append(i)
                    .append("})\n        MERGE (a)<-[:FUNDS]-(funding")
                    .append(i).append(")");
            agBuilder.add("funding" + i, asset.funding.get(i));
        }
        for(int i = 0 ; i < asset.issues.size(); i++) {
            sb.append("      MERGE (issues")
                    .append(i)
                    .append(":Issue{name: $issues")
                    .append(i)
                    .append("})\n        MERGE (a)-[:HAS]->(issues")
                    .append(i).append(")");
            agBuilder.add("issues" + i, asset.issues.get(i).issue());
        }
        for(int i = 0 ; i < asset.file_formats.size(); i++) {
            sb.append("      MERGE (new_file_format")
                    .append(i)
                    .append(":File_format{name: $new_file_format")
                    .append(i)
                    .append("})\n        MERGE (a)-[:HAS]->(new_file_format")
                    .append(i).append(")");
            agBuilder.add("new_file_format" + i, asset.file_formats.get(i));
        }
        for(int i = 0 ; i < asset.complete_digitiser_list.size(); i++) {
            sb.append("      MERGE (digitiser")
                    .append(i)
                    .append(":Digitiser{name: $digitiser")
                    .append(i)
                    .append("})\n        MERGE (a)<-[:WORKED_ON]-(digitiser")
                    .append(i).append(")");
            agBuilder.add("digitiser" + i, asset.complete_digitiser_list.get(i));
        }

        sb.append("""                         
                        \nMERGE (u:User{user_id: $user, name: $user})
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
                """);

        return sb.toString();
    }

//    default Asset persistAssetNew(Asset asset) {
//        System.out.println(asset);
//        //fileformats
//        //issues
//        //digitiser list
//        //funding
//        AgtypeListBuilder agtypeListBuilder = new AgtypeListBuilder();
//        asset.file_formats.forEach(agtypeListBuilder::add);
//        AgtypeMapBuilder tags = new AgtypeMapBuilder();
//        asset.tags.forEach((key, value) -> tags.add(key, value)); //(tag -> tags.add(tag));
//        AgtypeListBuilder restrictedAcces = new AgtypeListBuilder();
//        asset.restricted_access.forEach(role -> restrictedAcces.add(role.funding()));
//        AgtypeMapBuilder agBuilder = new AgtypeMapBuilder()
//                .add("institution_name", asset.institution)
//                .add("collection_name", asset.collection)
//                .add("workstation_name", asset.workstation)
//                .add("pipeline_name", asset.pipeline)
//                .add("internal_status", asset.internal_status.funding())
//                .add("status", asset.status)
//                .add("asset_guid", asset.asset_guid)
//                .add("asset_pid", asset.asset_pid)
////                        .add("funding", asset.funding)
////                        .add("payload_type", asset.payload_type)
////                        .add("file_formats", agtypeListBuilder.build())
//                .add("created_date", asset.created_date.toEpochMilli())
//                .add("user", asset.digitiser)
//                .add("tags", tags.build())
//                .add("asset_locked", asset.asset_locked)
//                .add("make_public", asset.make_public)
//                .add("push_to_specify", asset.push_to_specify);
//
//
//        String sql =
//                buildCreateSQL(asset, agBuilder);
//        logger.info("sql {}", sql);
//        try {
//            withHandle(handle -> {
//                AgtypeMap parms = agBuilder.build();
//                Agtype agtype = AgtypeFactory.create(parms);
//                handle.createUpdate(sql)
//                        .bind("params", agtype)
//                        .execute();
//                return handle;
//            });
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        return asset;
//    }
//


    default Asset updateAssetNoEventInternal(Asset asset) {
        String sql =
                """
                        UPDATE asset SET
                            internal_status = :internal_status
                            , asset_locked = :asset_locked
                            , error_message = :error_message
                            , error_timestamp = :error_timestamp
                        WHERE asset_guid = :asset_guid    
                """;
        try {
            withHandle(handle -> {
                handle.createUpdate(sql)
                        .bind("internal_status", asset.internal_status)
                        .bind("error_message", asset.error_message)
                        .bind("asset_locked", asset.asset_locked)
                        .bind("error_timestamp", asset.error_timestamp)
                        .bind("asset_guid", asset.asset_guid)
                        .execute();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return asset;
    }
    @Transaction
    default void executeUpdate(AGEQuery ageQuery) {
        try {
            withHandle(handle -> {
                Agtype agtype = AgtypeFactory.create(ageQuery.agtypeMapBuilder().build());
                handle.createUpdate(ageQuery.sql())
                        .bind("params", agtype)
                        .execute();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // We have to delete relations before update because deleting and recreating a relation without changes will result in
    // erratic behavior from the database. It appears that either relations will be created and not deleted or deleted and not recreated.
    static AGEQuery deleteRelations(Asset a) {
        AgtypeMapBuilder builder = new AgtypeMapBuilder();
        StringBuilder sb = new StringBuilder("""
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (asset:Asset {name: $asset_guid})
                            MATCH (asset)-[existing_has_status:HAS]->(status:Status)
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
                """);
        sb.append("""
                \n$$
                        , #params) as (a agtype);
                """);
        builder.add("asset_guid", a.asset_guid);
        return new AGEQuery(sb.toString(),builder);
    }
    static String buildUpdateSQL(Asset asset, AgtypeMapBuilder agBuilder) {
        StringBuilder sb = new StringBuilder("""
                SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (collection:Collection {name: $collection_name})
                            MATCH (workstation:Workstation {name: $workstation_name})
                            MATCH (pipeline:Pipeline {name: $pipeline_name})
                            MATCH (asset:Asset {name: $asset_guid})
                            MATCH (new_status:Status{name:$status})
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
            agBuilder.add("new_funding" + i, asset.funding.get(i));
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

    String UPDATE_ASSET_SQL = """
            UPDATE asset SET 
                status = :status
                , subject = :subject
                , payload_type = :payload_type
                , internal_status = :internal_status
                , tags = :tags::json
                , asset_locked = :asset_locked
                , make_public = :make_public
                , push_to_specify = :push_to_specify
            WHERE asset_guid = :asset_guid    
            """;
    default Asset update_asset_internal(Asset asset) {


        try {
            withHandle(handle -> {
                handle.createUpdate(UPDATE_ASSET_SQL)
                        .bind("status",asset.status)
                        .bind("subject",asset.subject)
                        .bind("payload_type", asset.payload_type)
//                        .bind("file_f")
                        .bind("internal_status", asset.internal_status)
                        .bind("tags", new Gson().toJson(asset.tags))
                        .bind("asset_locked", asset.asset_locked)
                        .bind("make_public", asset.make_public)
                        .bind("push_to_specify", asset.push_to_specify)
                        .bind("asset_guid",asset.asset_guid)
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

//    @Transaction
//    default void setEvent(String user, Event event, Asset asset) {
//        boilerplate();
//        internal_setEvent(user, event, asset);
//    }
//
//    default void internal_setEvent(String user, Event event, Asset asset) {
//
//    }

    default List<String> listSubjects() {
        boilerplate();
        return listSubjectsInternal();
    }

    default List<String> listSubjectsInternal() {
        String sql = """
                SELECT * FROM ag_catalog.cypher('dassco', $$
                    MATCH (a:Asset)
                    WHERE EXISTS(a.subject)
                    RETURN DISTINCT a.subject AS subject
                $$) as (subject agtype);
                """;

        try {
            return withHandle(handle -> {
                Connection connection = handle.getConnection();
                PgConnection pgConnection = connection.unwrap(PgConnection.class);
                pgConnection.addDataType("agtype", Agtype.class);
                return handle.createQuery(sql)
                        .map((rs, ctx) -> {
                            Agtype subject = rs.getObject("subject", Agtype.class);
                            return subject.getString();
                        }).list();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    default List<String> listPayloadTypes() {
        boilerplate();
        return listPayloadTypesInternal();
    }

    default List<String> listPayloadTypesInternal() {
        String sql = """
                SELECT * FROM ag_catalog.cypher('dassco', $$
                                    MATCH (a:Asset)
                                    WHERE EXISTS(a.payload_type)
                                    RETURN DISTINCT a.payload_type AS payload_type
                                $$) as (payload_type agtype);
                """;

        try {
            return withHandle(handle -> {
                Connection connection = handle.getConnection();
                PgConnection pgConnection = connection.unwrap(PgConnection.class);
                pgConnection.addDataType("agtype", Agtype.class);
                return handle.createQuery(sql)
                        .map((rs, ctx) -> {
                            Agtype subject = rs.getObject("payload_type", Agtype.class);
                            return subject.getString();
                        }).list();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    default List<String> listRestrictedAccess() {
        boilerplate();
        return listRestrictedAccessInternal();
    }

    default List<String> listRestrictedAccessInternal() {
        String sql = """
                SELECT DISTINCT restricted_access
                FROM ag_catalog.cypher('dassco', $$
                    MATCH (a:Asset)
                    WHERE EXISTS(a.restricted_access)
                    UNWIND a.restricted_access AS restricted_access
                    RETURN DISTINCT restricted_access
                $$) AS (restricted_access agtype);
                """;

        try {
            return withHandle(handle -> {
                Connection connection = handle.getConnection();
                PgConnection pgConnection = connection.unwrap(PgConnection.class);
                pgConnection.addDataType("agtype", Agtype.class);
                return handle.createQuery(sql)
                        .map((rs, ctx) -> {
                            Agtype restricted_access = rs.getObject("restricted_access", Agtype.class);
                            return restricted_access.getString();
                        }).list();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static final String READ_WITHOUT_WHERE =
            """
        MATCH (c:Collection)<-[:IS_PART_OF]-(asset)
        MATCH (create_event:Event{event:'CREATE_ASSET_METADATA'})<-[:CHANGED_BY]-(asset)
        MATCH (u:User)<-[:INITIATED_BY]-(create_event)
        MATCH (p:Pipeline)<-[:USED]-(create_event)
        MATCH (w:Workstation)<-[:USED]-(create_event)
        MATCH (i:Institution)<-[:BELONGS_TO]-(asset)
        MATCH (asset)-[:HAS]->(status:Status)
        MATCH (asset)-[:HAS]->(internal_status:Internal_status)
        OPTIONAL MATCH (s:Specimen)-[:USED_BY]->(:Asset{name: $asset_guid})
        OPTIONAL MATCH (asset)-[:CHILD_OF]->(pa:Asset)
        OPTIONAL MATCH (asset)<-[:WORKED_ON]-(digitisers:Digitiser)
        OPTIONAL MATCH (asset)<-[:DIGITISED]-(digitiser:Digitiser)
        OPTIONAL MATCH (asset)<-[:FUNDS]-(funding:Funding_entity)
        OPTIONAL MATCH (asset)-[:HAS]->(issue:Issue)
        OPTIONAL MATCH (asset)-[:HAS]->(payload_type:Payload_type)
        OPTIONAL MATCH (asset)-[:HAS]->(subject:Subject)
        OPTIONAL MATCH (asset)-[:HAS]->(camera_setting_control:Camera_setting_control)
        OPTIONAL MATCH (asset)-[:HAS]->(metadata_version:Metadata_version)
        OPTIONAL MATCH (asset)-[:HAS]->(metadata_source:Metadata_source)
        OPTIONAL MATCH (asset)-[:HAS]->(file_format:File_format)
        RETURN asset.asset_guid
        , asset.asset_pid
        , status.name
        , asset.multi_specimen
        , collect(distinct funding.name)
        , collect(distinct issue.name)
        , collect(distinct digitisers.name)
        , digitiser.name
        , subject.name
        , payload_type.name
        , collect(distinct file_format.name)
        , asset.asset_taken_date
        , internal_status.name
        , asset.asset_locked
        , pa.asset_guid
        , asset.tags
        , asset.error_message
        , asset.error_timestamp
        , collect(distinct s)
        , i.name
        , c.name
        , p.name
        , w.name
        , create_event.timestamp
        , asset.date_asset_finalised
        , u.name
        , asset.date_asset_taken
        , asset.date_metadata_ingested
        , null
        , camera_setting_control.name
        , metadata_version.name
        , asset.push_to_specify
        , asset.make_public
        , metadata_source.name
        $$, #params
        )
        as (asset_guid agtype
        , asset_pid agtype
        , status agtype
        , multi_specimen agtype
        , funding agtype
        , issues agtype
        , complete_digitiser_list agtype
        , digitiser agtype
        , subject agtype
        , payload_type agtype
        , file_formats agtype
        , asset_taken_date agtype
        , internal_status agtype
        , asset_locked agtype
        , parent_guid agtype
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
        , date_asset_taken agtype
        , date_metadata_ingested agtype
        , write_access agtype
        , camera_setting_control agtype
        , metadata_version agtype
        , push_to_specify agtype
        , make_public agtype
        , metadata_source agtype
        );
""";
}
