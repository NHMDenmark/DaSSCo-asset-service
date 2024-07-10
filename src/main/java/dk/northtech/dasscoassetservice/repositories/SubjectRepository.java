package dk.northtech.dasscoassetservice.repositories;

import jakarta.inject.Inject;
import org.apache.age.jdbc.base.Agtype;
import org.jdbi.v3.core.Jdbi;
import org.postgresql.jdbc.PgConnection;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

@Repository
public class SubjectRepository {

    private Jdbi jdbi;
    private DataSource dataSource;

    @Inject
    public SubjectRepository(Jdbi jdbi, DataSource dataSource){
        this.jdbi = jdbi;
        this.dataSource = dataSource;
    }

    private static final String boilerplate =
            "CREATE EXTENSION IF NOT EXISTS age;\n" +
                    "LOAD 'age';\n" +
                    "SET search_path = ag_catalog, \"$user\", public;";

    public List<String> listSubjects(){
        String sql = """
                SELECT * FROM ag_catalog.cypher('dassco', $$
                    MATCH (a:Asset)
                    WHERE EXISTS(a.subject)
                    RETURN DISTINCT a.subject AS subject
                $$) as (subject agtype);
                """;

        try {
            return jdbi.withHandle(handle -> {
                Connection connection = handle.getConnection();
                PgConnection pgConnection = connection.unwrap(PgConnection.class);
                pgConnection.addDataType("agtype", Agtype.class);
                handle.execute(boilerplate);
                return handle.createQuery(sql)
                        .map((rs, ctx) -> {
                            Agtype subject = rs.getObject("subject", Agtype.class);
                            return subject.getString();
                        }).list();
            });
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
