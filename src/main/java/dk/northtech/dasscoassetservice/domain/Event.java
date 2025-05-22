package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

import java.time.Instant;

public class Event {
    @Schema(description = "Username of the person that initiated the event", example = "test-username")
    public String user;
    @Schema(description = "Date and time when the event was initiated", example = "2023-05-24T00:00:00.000Z")
    public Instant timestamp;
    @Schema (description = "What happened to the asset", example = "DELETE_ASSET")
    public DasscoEvent event;
    @Schema(description = "The name of the pipeline that sent a create, update or delete request to the storage service", example = "ti-p1")
    public String pipeline;

    @JdbiConstructor
    public Event(String user, Instant timestamp, DasscoEvent event, String pipeline) {
        this.timestamp = timestamp;
        this.event = event;
        this.user = user;
        this.pipeline = pipeline;
    }

    public Event() {
    }

    public Event(String user, Instant timestamp, DasscoEvent event) {
        this.user = user;
        this.timestamp = timestamp;
        this.event = event;
    }

    @Override
    public String toString() {
        return "Event{" +
               "user='" + user + '\'' +
               ", timeStamp=" + timestamp +
               ", event=" + event +
               ", pipeline='" + pipeline + '\'' +
               '}';
    }
}
