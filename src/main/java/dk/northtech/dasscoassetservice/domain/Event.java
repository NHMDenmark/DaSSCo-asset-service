package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public class Event {
    @Schema(description = "Username of the person that initiated the event", example = "test-username")
    public String user;
    @Schema(description = "Date and time when the event was initiated", example = "2023-05-24T00:00:00.000Z")
    public Instant timeStamp;
    @Schema (description = "What happened to the asset", example = "DELETE_ASSET")
    public DasscoEvent event;
    @Schema(description = "The name of the pipeline that sent a create, update or delete request to the storage service", example = "ti-p1")
    public String pipeline;
    @Schema(description = "The name of the workstation used to do the imaging", example = "ti-ws1")
    public String workstation;

    public Event(String user, Instant timeStamp, DasscoEvent event, String pipeline, String workstation) {
        this.user = user;
        this.timeStamp = timeStamp;
        this.event = event;
        this.pipeline = pipeline;
        this.workstation = workstation;
    }

    public Event() {
    }

    public Event(String user, Instant timeStamp, DasscoEvent event) {
        this.user = user;
        this.timeStamp = timeStamp;
        this.event = event;
    }

    @Override
    public String toString() {
        return "Event{" +
               "user='" + user + '\'' +
               ", timeStamp=" + timeStamp +
               ", event=" + event +
               ", pipeline='" + pipeline + '\'' +
               ", workstation='" + workstation + '\'' +
               '}';
    }
}
