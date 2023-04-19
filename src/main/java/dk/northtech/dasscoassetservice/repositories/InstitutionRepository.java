package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Institute;
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
import java.util.List;
import java.util.Optional;

@Repository
public class InstitutionRepository {
    private Jdbi jdbi;
    private DataSource dataSource;

    private static final String boilerplate =
            "CREATE EXTENSION IF NOT EXISTS age;\n" +
                         "LOAD 'age';\n" +
                         "SET search_path = ag_catalog, \"$user\", public;";
    @Inject
    public InstitutionRepository(Jdbi jdbi, DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbi = jdbi;
    }


    public void persistInstitution(Institution institution) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MERGE (i:Institution {name: $name})
                            RETURN i.name
                        $$
                        , #params) as (name agtype);
                        """;


        try {
            jdbi.withHandle(handle -> {
//                Connection connection = handle.getConnection();
//                PgConnection pgConn = connection.unwrap(PgConnection.class);
//                pgConn.addDataType("agtype", Agtype.class);
                AgtypeMap name = new AgtypeMapBuilder().add("name", institution.name()).build();
                Agtype agtype = AgtypeFactory.create(name);
                handle.execute(boilerplate);
                handle.createUpdate(sql)
                        .bind("params", agtype)
                        .execute();
                handle.close();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Institution> listInstitutions() {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (i:Institution)
                            RETURN i.name
                        $$
                        ) as (name agtype);""";


        try {
            return jdbi.withHandle(handle -> {
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                handle.execute(boilerplate);
                return handle.createQuery(sql)
                        .map((rs, ctx) -> {
                            Agtype name = rs.getObject("name", Agtype.class);
                            return new Institution(name.getString());
                        }).list();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Institution> findInstitution(String institutionName) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (i:Institution)
                            WHERE i.name = $name
                            RETURN i.name
                        $$
                        , #params) as (name agtype);
                        """;


        try {
            return jdbi.withHandle(handle -> {
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                handle.execute(boilerplate);
                AgtypeMap institutionNameAG = new AgtypeMapBuilder().add("name", institutionName).build();
                Agtype agtype = AgtypeFactory.create(institutionNameAG);
                return handle.createQuery(sql).bind("params", agtype)
                        .map((rs, ctx) -> {
                            Agtype name = rs.getObject("name", Agtype.class);
                            return new Institution(name.getString());
                        }).findOne();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
