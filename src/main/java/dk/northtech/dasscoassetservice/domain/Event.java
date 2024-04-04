package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public class Event {
    public String user;
    public Instant timeStamp;
    public DasscoEvent event;
    @Schema(description = "name of the pipeline that started the event")
    public String pipeline;
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
