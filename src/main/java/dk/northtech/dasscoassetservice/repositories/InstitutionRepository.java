package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Institution;
import jakarta.inject.Inject;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.core.Jdbi;
import org.postgresql.jdbc.PgConnection;
import org.springframework.stereotype.Repository;


import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Repository
public class InstitutionRepository {
    private Jdbi jdbi;
    private DataSource dataSource;

    @Inject
    public InstitutionRepository(Jdbi jdbi, DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbi = jdbi;
    }


    public void persistInstitution(Institution institution) {
        String extension = "CREATE EXTENSION IF NOT EXISTS age;\n" +
                           "LOAD 'age';\n" +
                           "SET search_path = ag_catalog, \"$user\", public;";

        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MERGE (i:Institution {name: $name})
                            RETURN i.name
                        $$
                        , ?) as (name agtype);""".replace(":institution_name", institution.name());


        try(Connection connection = dataSource.getConnection()){
            PgConnection unwrap = connection.unwrap(PgConnection.class);
            unwrap.addDataType("agtype", Agtype.class);
            unwrap.execSQLUpdate(extension);
            try (PreparedStatement statement = unwrap.prepareStatement(sql)){
                AgtypeMap name = new AgtypeMapBuilder().add("name", institution.name()).build();
                Agtype agtype = AgtypeFactory.create(name);

                statement.setObject(1, agtype);
                statement.execute();
            }

//        jdbi.withHandle(handle -> {
//            Connection connection = handle.getConnection();
//            PgConnection pgConn = connection.unwrap(PgConnection.class);
//            pgConn.addDataType("agtype", Agtype.class);
//            handle.execute(extension);
//
//
//            handle.createUpdate(sql)
//                    .execute();
//            handle.close();
//            return handle;
//        });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


    }
}
