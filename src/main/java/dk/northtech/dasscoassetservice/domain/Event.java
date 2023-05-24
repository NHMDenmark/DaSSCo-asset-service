package dk.northtech.dasscoassetservice.domain;

import java.time.Instant;

public class Event {
    public String user;
    public Instant timeStamp;
    public DasscoEvent event;
    public String pipeline;
    public String workstation;

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
