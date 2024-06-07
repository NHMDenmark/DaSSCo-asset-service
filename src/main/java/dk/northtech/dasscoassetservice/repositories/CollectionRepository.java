package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
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
import java.util.List;
import java.util.Optional;

@Repository
public class CollectionRepository {
    private Jdbi jdbi;
    private DataSource dataSource;

    @Inject
    public CollectionRepository(Jdbi jdbi, DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbi = jdbi;
    }

    public void persistCollection(Collection collection) {
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
            jdbi.withHandle(handle -> {
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                AgtypeMap name = new AgtypeMapBuilder().add("institution_name", collection.institution()).add("collection_name", collection.name()).build();
                Agtype agtype = AgtypeFactory.create(name);
                handle.execute(DBConstants.AGE_BOILERPLATE);
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

    public List<Collection> listCollections(Institution institution) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                         , $$
                             MATCH (c:Collection)-[USED_BY]-(i:Institution)
                             WHERE i.name = $institution_name
                             RETURN c.name
                           $$
                         , #params
                        ) as (collection_name agtype);""";


        try {
            return jdbi.withHandle(handle -> {
                // We have to register the type
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);

                AgtypeMap name = new AgtypeMapBuilder().add("institution_name", institution.name()).build();
                Agtype agtype = AgtypeFactory.create(name);
                handle.execute(DBConstants.AGE_BOILERPLATE);
                return handle.createQuery(sql).bind("params", agtype)
                        .map((rs, ctx) -> {
                            Agtype collection_name = rs.getObject("collection_name", Agtype.class);
                            return new Collection(collection_name.getString(), institution.name());
                        }).list();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Collection> findCollection(String collectionName) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                         , $$
                             MATCH (c:Collection)
                             WHERE c.name = $collection_name
                             RETURN c.name
                           $$
                         , #params
                        ) as (collection_name agtype);""";


        try {
            return jdbi.withHandle(handle -> {
                // We have to register the type
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                AgtypeMap name = new AgtypeMapBuilder().add("collection_name", collectionName).build();
                Agtype agtype = AgtypeFactory.create(name);
                handle.execute(DBConstants.AGE_BOILERPLATE);
                return handle.createQuery(sql).bind("params", agtype)
                        .map((rs, ctx) -> {
                            Agtype collection_name = rs.getObject("collection_name", Agtype.class);
                            return new Collection(collection_name.getString(), null);
                        }).findOne();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
