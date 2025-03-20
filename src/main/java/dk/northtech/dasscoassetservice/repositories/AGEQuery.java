package dk.northtech.dasscoassetservice.repositories;

import org.apache.age.jdbc.base.type.AgtypeMapBuilder;

public record AGEQuery(String sql, AgtypeMapBuilder agtypeMapBuilder) {

}
