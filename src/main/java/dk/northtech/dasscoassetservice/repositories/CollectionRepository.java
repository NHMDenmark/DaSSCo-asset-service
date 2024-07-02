package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.repositories.helpers.CollectionMapper;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
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

public interface CollectionRepository extends SqlObject {
//    private Jdbi jdbi;
//    private DataSource dataSource;

//    @Inject
//    public CollectionRepository(Jdbi jdbi, DataSource dataSource) {
//        this.dataSource = dataSource;
//        this.jdbi = jdbi;
//    }

    @Transaction
    default void persistCollection(Collection collection) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MERGE (i:Institution {name: $institution_name})
                            MERGE (c:Collection {name: $collection_name})
                            MERGE (i)<-[:USED_BY]-(c)
                            RETURN i.name, c.name
                        $$
                        , #params) as (institution_name agtype, collection_name agtype);
                        """;


        try {
            withHandle(handle -> {
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                AgtypeMap name = new AgtypeMapBuilder().add("institution_name", collection.institution()).add("collection_name", collection.name()).build();
                Agtype agtype = AgtypeFactory.create(name);
                handle.execute(DBConstants.AGE_BOILERPLATE);
                handle.createUpdate(sql)
                        .bind("params", agtype)
                        .execute();
                RoleRepository roleRepository = handle.attach(RoleRepository.class);
                roleRepository.setRoleRestrictionCollection(collection, new Institution(collection.institution()), collection.roleRestrictions());

                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    default List<Collection> listCollections(Institution institution) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                         , $$
                             MATCH (c:Collection)-[USED_BY]-(i:Institution)
                             WHERE i.name = $institution_name
                             OPTIONAL MATCH (c)-[:RESTRICTED_TO]->(r:Role)
                             RETURN c.name
                             , i.name
                             , collect(r)
                           $$
                         , #params
                        ) as (collection_name agtype, institution_name agtype, roles agtype);""";


        try {
            return withHandle(handle -> {
                // We have to register the type
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);

                AgtypeMap name = new AgtypeMapBuilder().add("institution_name", institution.name()).build();
                Agtype agtype = AgtypeFactory.create(name);
                handle.execute(DBConstants.AGE_BOILERPLATE);
                return handle.createQuery(sql).bind("params", agtype)
                        .map(new CollectionMapper()).list();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    default Optional<Collection> findCollection(String collectionName, String institutionName) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                         , $$
                             MATCH (c:Collection{name: $collection_name})-[USED_BY]-(i:Institution{name: $institution_name})
                             OPTIONAL MATCH (c)-[:RESTRICTED_TO]->(r:Role)
                             RETURN c.name, i.name, collect(r)
                           $$
                         , #params
                        ) as (collection_name agtype, institution_name agtype, roles agtype);""";


        try {
            return withHandle(handle -> {
                // We have to register the type
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                AgtypeMap name = new AgtypeMapBuilder()
                        .add("collection_name", collectionName)
                        .add("institution_name", institutionName).build();
                Agtype agtype = AgtypeFactory.create(name);
                handle.execute(DBConstants.AGE_BOILERPLATE);
                return handle.createQuery(sql).bind("params", agtype)
                        .map(new CollectionMapper()).findOne();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    default List<Collection> readAll(){
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                         , $$
                             MATCH (c:Collection)-[USED_BY]-(i:Institution)
                             OPTIONAL MATCH (c)-[:RESTRICTED_TO]->(r:Role)
                             RETURN c.name
                             , i.name
                             , collect(r)
                           $$
                        ) as (collection_name agtype, institution_name agtype, roles agtype);""";


        try {
            return withHandle(handle -> {
                // We have to register the type
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                handle.execute(DBConstants.AGE_BOILERPLATE);
                return handle.createQuery(sql)
                        .map(new CollectionMapper()).list();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };
}
