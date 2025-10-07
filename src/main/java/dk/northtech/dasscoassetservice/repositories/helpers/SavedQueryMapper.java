package dk.northtech.dasscoassetservice.repositories.helpers;

import dk.northtech.dasscoassetservice.domain.SavedQuery;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SavedQueryMapper implements RowMapper<SavedQuery> {

    @Override
    public SavedQuery map(ResultSet rs, StatementContext ctx) throws SQLException {
        SavedQuery savedQuery = new SavedQuery();

        // Get query name (TEXT)
        savedQuery.name = rs.getString("name");
        if (rs.wasNull()) {
            savedQuery.name = null;
        }

        // Get query JSONB as text
        savedQuery.query = rs.getString("query");
        if (rs.wasNull()) {
            savedQuery.query = null;
        }

        return savedQuery;
    }
}