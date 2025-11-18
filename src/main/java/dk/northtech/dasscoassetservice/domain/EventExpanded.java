package dk.northtech.dasscoassetservice.domain;
import jakarta.annotation.Nullable;
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import java.time.Instant;
import java.util.List;

public class EventExpanded {

    private final String asset_guid;
    private final String event;
    private final Instant timestamp;
    private final String user;
    private final String pipeline;
    private final List<String> change_list;
    private final String bulk_update_uuid;

    @JdbiConstructor
    public EventExpanded(
            @ColumnName("asset_guid") String asset_guid,
            @ColumnName("event") String event,
            @ColumnName("timestamp") Instant timestamp,
            @ColumnName("user") String user,
            @ColumnName("pipeline") String pipeline,
            @Nullable @ColumnName("change_list") List<String> change_list,
            @Nullable @ColumnName("bulk_update_uuid") String bulk_update_uuid
    ) {
        this.asset_guid = asset_guid;
        this.event = event;
        this.timestamp = timestamp;
        this.user = user;
        this.pipeline = pipeline;
        this.change_list = change_list;
        this.bulk_update_uuid = bulk_update_uuid;
    }

    public String getAsset_guid() { return asset_guid; }
    public String getEvent() { return event; }
    public Instant getTimestamp() { return timestamp; }
    public String getUser() { return user; }
    public String getPipeline() { return pipeline; }
    public List<String> getChange_list() { return change_list; }
    public String getBulk_update_uuid() { return bulk_update_uuid; }
}