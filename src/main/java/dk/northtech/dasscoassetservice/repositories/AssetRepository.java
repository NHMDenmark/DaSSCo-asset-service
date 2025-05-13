package dk.northtech.dasscoassetservice.repositories;

import com.google.gson.Gson;
import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.DasscoEvent;
import dk.northtech.dasscoassetservice.domain.Event;
import dk.northtech.dasscoassetservice.repositories.helpers.AssetMapper;
import dk.northtech.dasscoassetservice.repositories.helpers.EventMapper;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                                    , file_formats
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
                                    , date_metadata_ingested
                                    , legality_id
                                    , mos_id
                                    , specify_attachment_title
                                    , specify_attachment_remarks
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
                                    , :dateMetadataIngested
                                    , :legality_id
                                    , :mos_id
                                    , :specify_attachment_title
                                    , :specify_attachment_remarks
                                  );
                    """;

    default void insertBaseAsset(Asset asset) {
        withHandle(handle -> {
            handle.createUpdate(INSERT_BASE_ASSET)
                    .bind("assetGuid", asset.asset_guid)
                    .bind("asset_pid", asset.asset_pid)
                    .bind("assetLocked", asset.asset_locked)
                    .bind("subject", asset.asset_subject != null ? asset.asset_subject.toLowerCase() : null)
                    .bind("collectionId", asset.collection_id)
                    .bind("digitiserId", asset.digitiser_id)
                    .bindArray("fileFormat", String.class, asset.file_formats)
                    .bind("multiSpecimen", asset.multi_specimen)
                    .bind("payloadType", asset.payload_type)
                    .bind("status", asset.status)
                    .bind("tags", new Gson().toJson(asset.tags)) // Assuming 'tags' is a Map or List of JSON-compatible types
                    .bind("workstationId", asset.workstation_id)
                    .bind("internalStatus", asset.internal_status)
                    .bind("makePublic", asset.make_public)
                    .bind("metadataSource", asset.metadata_source)
                    .bind("pushToSpecify", asset.push_to_specify)
                    .bind("metadataVersion", asset.metadata_version)
                    .bind("cameraSettingControl", asset.camera_setting_control)
                    .bind("date_asset_taken", asset.date_asset_taken != null ? Timestamp.from(asset.date_asset_taken) : null)
                    .bind("dateAssetFinalised", asset.date_asset_finalised != null ? Timestamp.from(asset.date_asset_finalised) : null)
                    .bind("dateMetadataIngested", asset.date_metadata_ingested != null ? Timestamp.from(asset.date_metadata_ingested) : null)
                    .bind("legality_id", asset.legality != null ? asset.legality.legality_id() : null)
                    .bind("mos_id", asset.mos_id)
                    .bind("specify_attachment_title", asset.specify_attachment_title)
                    .bind("specify_attachment_remarks", asset.specify_attachment_remarks)
                    .execute();
            return handle;
        });
    }

    ;

    @SqlUpdate("""
            INSERT INTO parent_child(child_guid, parent_guid)
            VALUES (:child_guid, :parent_guid)
            """)
    void insert_parent_child(String child_guid, String parent_guid);

    @SqlUpdate("""
                DELETE FROM parent_child 
                WHERE parent_guid = :parent_guid AND child_guid = :child_guid
            """)
    void delete_parent_child(String child_guid, String parent_guid);

    @SqlQuery("""
                SELECT parent_guid FROM parent_child WHERE child_guid = :child_id;
            """)
    Set<String> getParents(String child_id);


    String READ_ASSET = """
        SELECT asset.*
            , collection.collection_name
            , collection.institution_name
            , dassco_user.username AS digitiser
            , workstation.workstation_name 
            , copyright
            , license
            , credit
        FROM asset
        LEFT JOIN collection USING(collection_id)
        LEFT JOIN workstation USING(workstation_id)  
        LEFT JOIN legality USING(legality_id)
        LEFT JOIN dassco_user ON dassco_user.dassco_user_id = asset.digitiser_id
        """;

    @Transaction
    default Optional<Asset> readAsset(String assetId) {

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


    String UPDATE_ASSET_SQL = """
            UPDATE asset SET 
                status = :status
                , asset_pid = :asset_pid
                , subject = :subject
                , payload_type = :payload_type
                , internal_status = :internal_status
                , tags = :tags::json
                , asset_locked = :asset_locked
                , make_public = :make_public
                , file_formats = :file_formats
                , push_to_specify = :push_to_specify
                , digitiser_id = :digitiser_id
                , metadata_version = :metadata_version
                , metadata_source = :metadata_source
                , date_metadata_ingested = :date_metadata_ingested
                , legality_id = :legality_id
                , mos_id = :mos_id
                , camera_setting_control = :camera_setting_control
                , specify_attachment_title = :specify_attachment_title
                , specify_attachment_remarks = :specify_attachment_remarks
            WHERE asset_guid = :asset_guid    
            """;

    default Asset update_asset_internal(Asset asset) {
        try {
            withHandle(handle -> {
                handle.createUpdate(UPDATE_ASSET_SQL)
                        .bind("asset_pid", asset.asset_pid)
                        .bind("status", asset.status)
                        .bind("subject", asset.asset_subject)
                        .bind("payload_type", asset.payload_type)
                        .bind("digitiser_id", asset.digitiser_id)
//                        .bind("file_f")
                        .bind("internal_status", asset.internal_status)
                        .bindArray("file_formats", String.class, asset.file_formats)
                        .bind("tags", new Gson().toJson(asset.tags))
                        .bind("asset_locked", asset.asset_locked)
                        .bind("make_public", asset.make_public)
                        .bind("push_to_specify", asset.push_to_specify)
                        .bind("asset_guid", asset.asset_guid)
                        .bind("metadata_version", asset.metadata_version)
                        .bind("metadata_source", asset.metadata_source)
                        .bind("legality_id", asset.legality == null ? null : asset.legality.legality_id())
                        .bind("mos_id", asset.mos_id)
                        .bind("date_metadata_ingested", asset.date_metadata_ingested)
                        .bind("camera_setting_control", asset.camera_setting_control)
                        .bind("specify_attachment_title", asset.specify_attachment_title)
                        .bind("specify_attachment_remarks", asset.specify_attachment_remarks)
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
        // Deletes Asset and removes connections to Specimens and Events.
        // The query then removes orphaned Specimens and Events (Specimens and Events not connected to any Asset).
        String delete_asset_specimen = "DELETE FROM asset_specimen WHERE asset_guid = :assetGuid RETURNING specimen_id;";
        String delete_specimen = """
                    DELETE FROM specimen
                    WHERE specimen_id IN (<ids>)
                        AND NOT EXISTS (
                            SELECT 1 FROM asset_specimen asp WHERE asp.specimen_id = specimen.specimen_id
                        )
                """;
        String delete_digitisers = "DELETE FROM digitiser_list WHERE asset_guid = :assetGuid;";
        String delete_publication_link = "DELETE FROM asset_publisher WHERE asset_guid = :assetGuid;";
        String delete_asset_group_asset = "DELETE FROM asset_group_asset WHERE asset_guid = :assetGuid;";
        String delete_events = "DELETE FROM event WHERE asset_guid = :assetGuid;";
        String delete_issue = "DELETE FROM issue WHERE asset_guid = :assetGuid;";
        String delete_file = "DELETE FROM file WHERE asset_guid = :assetGuid;";
        String delete_asset_funding = "DELETE FROM asset_funding WHERE asset_guid = :assetGuid";
        String delete_parent_child = """
                    DELETE FROM parent_child 
                    WHERE parent_guid = :parent_guid OR child_guid = :child_guid 
                """;
        String delete_asset_metadata = "DELETE FROM asset WHERE asset_guid = :assetGuid";
        String delete_funding = """
                    DELETE FROM funding
                    WHERE funding.funding_id IN (
                    SELECT funding_id
                        FROM funding
                        LEFT JOIN asset_funding USING(funding_id)
                        WHERE asset_guid IS null
                    )
                """;
        withHandle(h -> {
            List<Integer> ids = h.createQuery(delete_asset_specimen)
                    .bind("assetGuid", assetGuid).mapTo(Integer.class).list();
            h.createUpdate(delete_specimen).bindList("ids", ids).execute();
            h.createUpdate(delete_digitisers).bind("assetGuid", assetGuid).execute();
            h.createUpdate(delete_publication_link).bind("assetGuid", assetGuid).execute();
            h.createUpdate(delete_asset_group_asset).bind("assetGuid", assetGuid).execute();
            h.createUpdate(delete_events).bind("assetGuid", assetGuid).execute();
            h.createUpdate(delete_issue).bind("assetGuid", assetGuid).execute();
            h.createUpdate(delete_file).bind("assetGuid", assetGuid).execute();
            h.createUpdate(delete_asset_funding).bind("assetGuid", assetGuid).execute();
            h.createUpdate(delete_funding).execute();
            h.createUpdate(delete_parent_child).bind("parent_guid", assetGuid).bind("child_guid", assetGuid).execute();
            h.createUpdate(delete_asset_metadata).bind("assetGuid", assetGuid).execute();
            return h;
        });
    }


}
