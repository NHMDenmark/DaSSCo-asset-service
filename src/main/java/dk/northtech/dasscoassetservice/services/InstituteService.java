package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Institute;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.statement.HashPrefixSqlParser;
import org.jdbi.v3.jackson2.Jackson2Plugin;
import org.jdbi.v3.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;

@Service
public class InstituteService {
    private final Jdbi jdbi;
    private static final Logger log = LoggerFactory.getLogger(InstituteService.class);

    @Inject
    public InstituteService(DataSource dataSource) {
        this.jdbi = Jdbi.create(dataSource)
                .registerRowMapper((ConstructorMapper.factory(Institute.class)))
                .installPlugin(new Jackson2Plugin());;
    }

    public List<Institute> getInstitutes() {
        QualifiedType<Institute> qualifiedType = QualifiedType.of(Institute.class).with(Json.class);
        String extension = "CREATE EXTENSION IF NOT EXISTS age;\n" +
            "LOAD 'age';\n" +
            "SET search_path = ag_catalog, \"$user\", public;";

        jdbi.withHandle(h -> h.execute(extension));

        String sql = "SELECT * from cypher('dassco', $$\n" +
                "        MATCH (i:Institute)\n" +
                "        RETURN i\n" +
                "$$) as (i agtype);";

        return jdbi.withHandle(h ->
                h.setSqlParser(new HashPrefixSqlParser())
                .createQuery(sql)
                .mapTo(qualifiedType)
                .list()
        );
    }
}
