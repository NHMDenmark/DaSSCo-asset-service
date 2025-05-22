package dk.northtech.dasscoassetservice.repositories.helpers;

import org.apache.age.jdbc.base.Agtype;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class EnumMapper implements RowMapper<String> {


    @Override
    public String map(ResultSet rs, StatementContext ctx) throws SQLException {
        Agtype name = rs.getObject("e", Agtype.class);
        return name.getString();
    }
}
