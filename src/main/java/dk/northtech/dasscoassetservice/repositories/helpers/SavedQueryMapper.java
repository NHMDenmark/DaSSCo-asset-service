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

        Agtype title = rs.getObject("title", Agtype.class);
        Agtype query = rs.getObject("query", Agtype.class);
        savedQuery.query = query.getString();
        savedQuery.title = title.getString();

        return savedQuery;
    }
}
