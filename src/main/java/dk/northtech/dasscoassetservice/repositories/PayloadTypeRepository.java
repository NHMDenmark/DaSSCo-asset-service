package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.services.PayloadTypeService;
import jakarta.inject.Inject;
import org.apache.age.jdbc.base.Agtype;
import org.jdbi.v3.core.Jdbi;
import org.postgresql.jdbc.PgConnection;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

@Repository
public class PayloadTypeRepository {
    private Jdbi jdbi;
    private DataSource dataSource;

    @Inject
    public PayloadTypeRepository(Jdbi jdbi, DataSource dataSource){
        this.jdbi = jdbi;
        this.dataSource = dataSource;
    }

    private static final String boilerplate =
            "CREATE EXTENSION IF NOT EXISTS age;\n" +
                    "LOAD 'age';\n" +
                    "SET search_path = ag_catalog, \"$user\", public;";

    public List<String> listPayloadTypes(){
        String sql = """
                SELECT * FROM ag_catalog.cypher('dassco', $$
                                    MATCH (a:Asset)
                                    WHERE EXISTS(a.payload_type)
                                    RETURN DISTINCT a.payload_type AS payload_type
                                $$) as (payload_type agtype);
                """;

        try {
            return jdbi.withHandle(handle -> {
                Connection connection = handle.getConnection();
                PgConnection pgConnection = connection.unwrap(PgConnection.class);
                pgConnection.addDataType("agtype", Agtype.class);
                handle.execute(boilerplate);
                return handle.createQuery(sql)
                        .map((rs, ctx) -> {
                            Agtype subject = rs.getObject("payload_type", Agtype.class);
                            return subject.getString();
                        }).list();
            });
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
