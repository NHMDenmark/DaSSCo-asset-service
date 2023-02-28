package dk.northtech.dasscoassetservice.services;

import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.util.List;

@Service
public class SpecimenService {
    private final Jdbi jdbi;

    @Inject
    public SpecimenService(DataSource dataSource) {
        this.jdbi = Jdbi.create(dataSource);
    }

    public List<String> getSpecimenData() {
        String sql = "CREATE EXTENSION IF NOT EXISTS age;\n" +
                "LOAD 'age';\n" +
                "SET search_path = ag_catalog, \"$user\", public;\n" +
                "SELECT * from cypher('dassco', $$" +
                " MATCH (V)-[R:PROPERTY_OF]-(V2)-[K:HAS_MEDIA]-(I)" +
                " RETURN V,R,V2,K,I" +
                " $$) as (V agtype, R agtype, V2 agtype, K agtype, I agtype);";
        return jdbi.withHandle(h -> h.createUpdate(sql)
                .executeAndReturnGeneratedKeys()
                .mapTo(String.class)
                .list()
        );
    }
}
