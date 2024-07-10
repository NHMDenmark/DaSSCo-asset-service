package dk.northtech.dasscoassetservice.repositories.helpers;

import dk.northtech.dasscoassetservice.domain.*;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.type.AgtypeList;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InstitutionMapper implements RowMapper<Institution> {

    @Override
    public Institution map(ResultSet rs, StatementContext ctx) throws SQLException {
        Agtype name = rs.getObject("name", Agtype.class);
        Agtype specimens = rs.getObject("roles", Agtype.class);
        List<Role> roles =  specimens.getList().stream()
                .map(x ->mapRole((AgtypeMap) x)).collect(Collectors.toList());
        return new Institution(name.getString(), roles);
    }
    Role mapRole(AgtypeMap agtype) {
        AgtypeMap properties = agtype.getMap("properties");
        return new Role(properties.getString("name"));
    }
}