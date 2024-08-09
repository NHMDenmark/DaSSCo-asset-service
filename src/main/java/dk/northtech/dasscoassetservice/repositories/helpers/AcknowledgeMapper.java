package dk.northtech.dasscoassetservice.repositories.helpers;

import dk.northtech.dasscoassetservice.domain.*;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class AcknowledgeMapper implements RowMapper<Acknowledge> {

    @Override
    public Acknowledge map(ResultSet rs, StatementContext ctx) throws SQLException {

        Agtype guids = rs.getObject("asset_guids", Agtype.class);
        Agtype status = rs.getObject("status", Agtype.class);
        Agtype body = rs.getObject("body", Agtype.class);
        Agtype date = rs.getObject("date", Agtype.class);

        List<String> assetGuids = guids.getList().stream().map(Object::toString).collect(Collectors.toList());

        return new Acknowledge(assetGuids, AcknowledgeStatus.valueOf(status.getString()), body.getString(), Instant.ofEpochMilli(date.getLong()));
    }
}
