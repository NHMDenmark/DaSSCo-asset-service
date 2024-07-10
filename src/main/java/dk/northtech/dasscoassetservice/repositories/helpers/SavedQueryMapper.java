package dk.northtech.dasscoassetservice.repositories.helpers;

import dk.northtech.dasscoassetservice.domain.DasscoEvent;
import dk.northtech.dasscoassetservice.domain.Event;
import dk.northtech.dasscoassetservice.domain.SavedQuery;
import org.apache.age.jdbc.base.Agtype;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

public class SavedQueryMapper implements RowMapper<SavedQuery> {

    @Override
    public SavedQuery map(ResultSet rs, StatementContext ctx) throws SQLException {
        SavedQuery savedQuery = new SavedQuery();

        rs.getObject("query_name");
        if (!rs.wasNull()) {
            Agtype name = rs.getObject("query_name", Agtype.class);
            savedQuery.name = name.getString();
        } else {
            savedQuery.name = "null";
        }

        rs.getObject("query_query");
        if (!rs.wasNull()) {
            Agtype query = rs.getObject("query_query", Agtype.class);
            savedQuery.query = query.getString();
        } else {
            savedQuery.query = "null";
        }

        return savedQuery;
    }
}
