package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Digitiser;
import dk.northtech.dasscoassetservice.domain.User;
import jakarta.inject.Inject;
import org.apache.age.jdbc.base.Agtype;
import org.jdbi.v3.core.Jdbi;
import org.postgresql.jdbc.PgConnection;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

@Repository
public class DigitiserRepository {
    private Jdbi jdbi;
    private DataSource dataSource;

    private static final String boilerplate =
            "CREATE EXTENSION IF NOT EXISTS age;\n" +
                    "LOAD 'age';\n" +
                    "SET search_path = ag_catalog, \"$user\", public;";

    @Inject
    public DigitiserRepository(Jdbi jdbi, DataSource dataSource){
        this.jdbi = jdbi;
        this.dataSource = dataSource;
    }

    public List<Digitiser> listDigitisers(){
        String sql = """
                SELECT * FROM ag_catalog.cypher('dassco'
                                    , $$
                                        MATCH (u:User)
                                        WHERE u.name IS NOT NULL AND u.user_id IS NOT NULL
                                        RETURN u.name, u.user_id
                                    $$) as (name agtype, user_id agtype);
                """;

        try {
            return jdbi.withHandle(handle -> {
                Connection connection = handle.getConnection();
                PgConnection pgConnection = connection.unwrap(PgConnection.class);
                pgConnection.addDataType("agtype", Agtype.class);
                handle.execute(boilerplate);
                return handle.createQuery(sql)
                        .map((rs, ctx) -> {
                            Agtype name = rs.getObject("name", Agtype.class);
                            Agtype userId = rs.getObject("user_id", Agtype.class);
                            return new Digitiser(userId.getString(), name.getString());
                        }).list();
            });
        } catch (Exception e){
          throw new RuntimeException(e);
        }
    }
}
