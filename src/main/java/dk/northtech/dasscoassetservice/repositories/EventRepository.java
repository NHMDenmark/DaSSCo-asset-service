package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.DasscoEvent;
import dk.northtech.dasscoassetservice.domain.Event;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.time.Instant;
import java.util.List;

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
    FROM event 
        LEFT JOIN pipeline USING (pipeline_id)
        LEFT JOIN dassco_user USING (dassco_user_id)
    WHERE asset_guid = :assetGuid
    ORDER BY timestamp DESC
""")
    List<Event> getAssetEvents(String assetGuid);
}
