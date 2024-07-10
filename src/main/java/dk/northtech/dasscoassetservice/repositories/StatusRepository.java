package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.AssetStatus;
import jakarta.inject.Inject;
import org.apache.age.jdbc.base.Agtype;
import org.jdbi.v3.core.Jdbi;
import org.postgresql.jdbc.PgConnection;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;


@Repository
public class StatusRepository {
    private Jdbi jdbi;
    private DataSource dataSource;

    @Inject
    public StatusRepository(Jdbi jdbi, DataSource dataSource){
        this.jdbi = jdbi;
        this.dataSource = dataSource;
    }

    private static final String boilerplate =
            "CREATE EXTENSION IF NOT EXISTS age;\n" +
                    "LOAD 'age';\n" +
                    "SET search_path = ag_catalog, \"$user\", public;";

    public List<AssetStatus> listStatus(){
        String sql = """
                SELECT * FROM ag_catalog.cypher('dassco', $$
                    MATCH (a:Asset)
                    WHERE EXISTS(a.status)
                    RETURN DISTINCT a.status AS status
                $$) as (status agtype);
                """;

        try {
            return jdbi.withHandle(handle -> {
                Connection connection = handle.getConnection();
                PgConnection pgConnection = connection.unwrap(PgConnection.class);
                pgConnection.addDataType("agtype", Agtype.class);
                handle.execute(boilerplate);
                return handle.createQuery(sql)
                        .map((rs, ctx) -> {
                            Agtype subject = rs.getObject("status", Agtype.class);
                            return AssetStatus.valueOf(subject.getString());
                        }).list();
            });
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
