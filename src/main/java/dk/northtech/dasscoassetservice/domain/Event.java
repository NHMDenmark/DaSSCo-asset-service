package dk.northtech.dasscoassetservice.domain;

import java.time.Instant;

public class Event {
    public String user;
    public Instant timeStamp;
    public DasscoEvent event;
    public String pipeline;
    public String workstation;
}
