package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.Role;
import dk.northtech.dasscoassetservice.repositories.helpers.InstitutionMapper;
import jakarta.inject.Inject;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.postgresql.jdbc.PgConnection;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface InstitutionRepository extends SqlObject {


     static final String boilerplate =
            "CREATE EXTENSION IF NOT EXISTS age;\n" +
                         "LOAD 'age';\n" +
                         "SET search_path = ag_catalog, \"$user\", public;";


    @Transaction
    default void persistInstitution(Institution institution) {
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
            withHandle(handle -> {

//                Connection connection = handle.getConnection();
//                PgConnection pgConn = connection.unwrap(PgConnection.class);
//                pgConn.addDataType("agtype", Agtype.class);
                AgtypeMap name = new AgtypeMapBuilder().add("name", institution.name()).build();
                Agtype agtype = AgtypeFactory.create(name);
                handle.execute(boilerplate);
                handle.createUpdate(sql)
                        .bind("params", agtype)
                        .execute();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    default List<Institution> listInstitutions() {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (i:Institution)
                            OPTIONAL MATCH (i)-[:RESTRICTED_TO]->(r:Role)
                            RETURN i.name
                            , collect(r)
                        $$
                        ) as (name agtype, roles agtype);""";


        try {
            return withHandle(handle -> {
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                handle.execute(boilerplate);
                return handle.createQuery(sql)
                        .map(new InstitutionMapper()).list();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    default Optional<Institution> findInstitution(String institutionName) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (i:Institution{name: $name})
                            OPTIONAL MATCH (i)-[:RESTRICTED_TO]->(r:Role)
                            RETURN i.name, collect(r)
                        $$
                        , #params) as (name agtype, roles agtype);
                        """;


        try {
            return withHandle(handle -> {
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                handle.execute(boilerplate);
                AgtypeMap institutionNameAG = new AgtypeMapBuilder().add("name", institutionName).build();
                Agtype agtype = AgtypeFactory.create(institutionNameAG);
                return handle.createQuery(sql).bind("params", agtype)
                        .map(new InstitutionMapper()).findOne();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
