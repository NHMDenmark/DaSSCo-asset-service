package dk.northtech.dasscoassetservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.Institute;
import dk.northtech.dasscoassetservice.domain.JsonObject;
import dk.northtech.dasscoassetservice.domain.Specimen;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.JoinRowMapper;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.statement.HashPrefixSqlParser;
import org.jdbi.v3.jackson2.Jackson2Plugin;
import org.jdbi.v3.json.Json;
import org.jdbi.v3.json.JsonMapper;
import org.springframework.stereotype.Service;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class SpecimenService {
    private final Jdbi jdbi;

    @Inject
    public SpecimenService(DataSource dataSource) {
        this.jdbi = Jdbi.create(dataSource)
                .registerRowMapper((ConstructorMapper.factory(JsonObject.class)))
                .registerRowMapper((ConstructorMapper.factory(Institute.class)))
                .registerRowMapper((ConstructorMapper.factory(Asset.class)))
                .registerRowMapper((ConstructorMapper.factory(Specimen.class)))
                .installPlugin(new Jackson2Plugin());
    }

    public List<JsonObject> getSpecimenData() {
        QualifiedType<JsonObject> qualifiedType = QualifiedType.of(JsonObject.class).with(Json.class);
        String extension = "CREATE EXTENSION IF NOT EXISTS age;\n" +
            "LOAD 'age';\n" +
            "SET search_path = ag_catalog, \"$user\", public;";

        String sql = "SELECT * from cypher('dassco', $$" +
            " MATCH (institute)-[R:PROPERTY_OF]-(asset)-[K:HAS_MEDIA]-(specimen)" +
            " RETURN institute, asset, specimen" +
            " $$) as (institute agtype, asset agtype, specimen agtype);";

        jdbi.withHandle(h -> h.execute(extension));
        return jdbi.withHandle(h ->
                h.setSqlParser(new HashPrefixSqlParser())
                .createQuery(sql)
                .mapTo(qualifiedType)
                .list()
        );
    }
}
