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

    @SqlQuery("""
    SELECT event
        , timestamp
        , username AS user
        , pipeline_name AS pipeline
    FROM event 
        LEFT JOIN pipeline USING (pipeline_id)
        LEFT JOIN dassco_user USING (dassco_user_id)
    WHERE asset_guid = :assetGuid
""")
    List<Event> getAssetEvents(String assetGuid);
}
