package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.DasscoEvent;
import dk.northtech.dasscoassetservice.domain.Event;
import dk.northtech.dasscoassetservice.domain.EventExpanded;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EventRepository extends SqlObject {

    @SqlUpdate("""
            INSERT INTO event(asset_guid, event, dassco_user_id, pipeline_id) 
            VALUES ( :assetGuid, :event,  :userId, :pipelineId)
            """)
    public void insertEvent(String assetGuid, DasscoEvent event, Integer userId, Integer pipelineId);

    @SqlUpdate("""
            INSERT INTO event(asset_guid, event, dassco_user_id, pipeline_id, change_list)
            VALUES ( :assetGuid, :event,  :userId, :pipelineId, :change_list)
            """)
    public void insertEvent(String assetGuid, DasscoEvent event, Integer userId, Integer pipelineId, List<String> change_list);

    @SqlQuery("""
                SELECT event
                    , timestamp
                    , username AS user
                    , pipeline_name AS pipeline
                    , change_list
                    , bulk_update_uuid
                FROM event 
                    LEFT JOIN pipeline USING (pipeline_id)
                    LEFT JOIN dassco_user USING (dassco_user_id)
                WHERE asset_guid = :assetGuid
                ORDER BY timestamp DESC
            """)
    List<Event> getAssetEvents(String assetGuid);



    @SqlQuery("""
    SELECT
        e.asset_guid AS asset_guid,
        e.event AS event,
        e.timestamp AS timestamp,
        du.username AS "user",
        p.pipeline_name AS pipeline,
        e.change_list AS change_list,
        e.bulk_update_uuid AS bulk_update_uuid
    FROM event e
    LEFT JOIN pipeline p USING (pipeline_id)
    LEFT JOIN dassco_user du USING (dassco_user_id)
    WHERE e.event = :type
      AND (:startDate::timestamp IS NULL OR e.timestamp >= :startDate::timestamp)
      AND (:endDate::timestamp IS NULL OR e.timestamp <= :endDate::timestamp)
    ORDER BY e.timestamp <direction>
    LIMIT :limit OFFSET :offset
""")
    @RegisterConstructorMapper(EventExpanded.class)
    List<EventExpanded> getEvents(
            @Bind("type") String type,
            @Bind("startDate") Instant startDate,
            @Bind("endDate") Instant endDate,
            @Bind("limit") int limit,
            @Bind("offset") int offset,
            @Define("direction") String direction
    );

    @SqlQuery("""
        SELECT COUNT(*)
        FROM event
        WHERE event = :type
          AND (:startDate::timestamp IS NULL OR timestamp >= :startDate::timestamp)
          AND (:endDate::timestamp IS NULL OR timestamp <= :endDate::timestamp)
        """)
    long countEvents(
            @Bind("type") String type,
            @Bind("startDate") Instant startDate,
            @Bind("endDate") Instant endDate
    );

    @SqlQuery("SELECT event FROM event_type ORDER BY event")
    List<String> getEventTypes();
}
