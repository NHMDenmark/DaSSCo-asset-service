package dk.northtech.dasscoassetservice.repositories.helpers;

import dk.northtech.dasscoassetservice.domain.DasscoEvent;
import dk.northtech.dasscoassetservice.domain.Event;
import org.apache.age.jdbc.base.Agtype;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

public class EventMapper implements RowMapper<Event> {

    @Override
    public Event map(ResultSet rs, StatementContext ctx) throws SQLException {
        Event event = new Event();
        Agtype eventType = rs.getObject("event", Agtype.class);
        Agtype eventTimestamp = rs.getObject("timestamp", Agtype.class);
        event.event = DasscoEvent.valueOf(eventType.getString());
        event.timeStamp = Instant.ofEpochMilli(eventTimestamp.getLong());

        // We will get a null pointer if we try to read a null Agtype from the result. This is a workaround
        rs.getString("workstation");
        if (!rs.wasNull()) {
            Agtype workstation = rs.getObject("workstation", Agtype.class);
            event.workstation = workstation.getString();
        }
        rs.getString("pipeline");
        if (!rs.wasNull()) {
            Agtype pipeline = rs.getObject("pipeline", Agtype.class);
            event.pipeline = pipeline.getString();
        }
        rs.getString("event_user");
        if(!rs.wasNull()){
            Agtype userName = rs.getObject("event_user", Agtype.class);
            event.user = userName.getString();
        }
        return event;
    }
}
