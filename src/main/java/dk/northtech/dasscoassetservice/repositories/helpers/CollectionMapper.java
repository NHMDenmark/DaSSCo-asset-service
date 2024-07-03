package dk.northtech.dasscoassetservice.repositories.helpers;

import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.Role;
import dk.northtech.dasscoassetservice.domain.Specimen;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class CollectionMapper implements RowMapper<Collection> {

    @Override
    public Collection map(ResultSet rs, StatementContext ctx) throws SQLException {
        Agtype name = rs.getObject("collection_name", Agtype.class);
        Agtype institutionName = rs.getObject("institution_name", Agtype.class);
        Agtype rolesAg = rs.getObject("roles", Agtype.class);
        List<Role> roles =  rolesAg.getList().stream()
                .map(x -> mapRole((AgtypeMap) x)).collect(Collectors.toList());
        return new Collection(name.getString(), institutionName.getString(), roles);
    }


    Role mapRole(AgtypeMap agtype) {
        AgtypeMap properties = agtype.getMap("properties");
        return new Role(properties.getString("name"));
    }
}
