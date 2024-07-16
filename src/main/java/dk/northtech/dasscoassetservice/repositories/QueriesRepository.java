package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.NodeProperty;
import dk.northtech.dasscoassetservice.domain.SavedQuery;
import dk.northtech.dasscoassetservice.repositories.helpers.AssetMapper;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import dk.northtech.dasscoassetservice.repositories.helpers.SavedQueryMapper;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.sqlobject.SqlObject;
import org.postgresql.jdbc.PgConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public interface QueriesRepository extends SqlObject {

    default void boilerplate() {
        withHandle(handle -> {
            Connection connection = handle.getConnection();
            try {
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                handle.execute(DBConstants.AGE_BOILERPLATE);
                return handle;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    default Map<String, List<String>> getNodeProperties() {
        boilerplate();
//                WHERE NOT 'Event' IN labels(n)
        String sql =
            """
                SELECT * FROM ag_catalog.cypher('dassco', $$
                MATCH (n)
                WHERE NOT 'Event' IN labels(n) AND NOT 'User' IN labels(n)
                WITH labels(n) AS lbl, keys(n) AS keys, size(keys(n)) AS key_count
                UNWIND lbl AS label
                WITH label, keys, key_count
                ORDER BY key_count desc
                WITH label, head(collect(keys)) AS properties
                RETURN label, properties
                     $$
              ) as (label agtype, properties agtype);
            """;

        return withHandle(handle -> {
            return handle.createQuery(sql)
                .registerRowMapper(ConstructorMapper.factory(NodeProperty.class))
                .mapTo(NodeProperty.class)
                .collect(Collector.of(HashMap::new, (accum, item) -> {
                    item.properties = item.properties.replaceAll("[\\[\\]\"]", "");
                    List<String> propList = Arrays.stream(item.properties.split(",")).map(String::trim).collect(Collectors.toList());
                    accum.put(item.label.replace("\"", ""), propList);
                }, (l, r) -> {
                    l.putAll(r);
                    return l;
                }, Collector.Characteristics.IDENTITY_FINISH));
        });
    }

    default List<Asset> getAssetsFromQuery(String query) {
        boilerplate();
        return withHandle(handle -> {
            return handle.createQuery(query)
                    .map(new AssetMapper())
                    .list();
        });
    }

    default int getAssetCountFromQuery(String query) {
        boilerplate();
        return withHandle(handle -> {
            return handle.createQuery(query)
                    .mapTo(Integer.class)
                    .one();
        });
    }

    default SavedQuery saveQuery(SavedQuery savedQuery, String username) {
        boilerplate();
        String sql = """
                SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                        MATCH (u:User {name: $username})
                        MERGE (q:Query {name: $name, query: $query})
                        MERGE (u)<-[:SAVED_BY]-(q)
                        RETURN q.name, q.query
                    $$
                    , #params)
                    as (query_name agtype, query_query agtype);
                """;

        return withHandle(handle -> {
            AgtypeMap params = new AgtypeMapBuilder()
                    .add("username", username)
                    .add("name", savedQuery.name)
                    .add("query", savedQuery.query).build();
            Agtype agtype = AgtypeFactory.create(params);
            return handle.createQuery(sql)
                    .bind("params", agtype)
                    .map(new SavedQueryMapper())
                    .one();
        });
    }

    default List<SavedQuery> getSavedQueries(String username) {
        boilerplate();
        String sql = """
                SELECT * FROM ag_catalog.cypher('dassco'
                   , $$
                         MATCH (u:User {name: $username})<-[:SAVED_BY]-(q:Query)
                         return q.name, q.query
                     $$
                   , #params) as (query_name agtype, query_query agtype);
                """;

        return withHandle(handle -> {
            AgtypeMap params = new AgtypeMapBuilder()
                    .add("username", username).build();
            Agtype agtype = AgtypeFactory.create(params);
            return handle.createQuery(sql)
                    .bind("params", agtype)
                    .map(new SavedQueryMapper())
                    .list();
        });
    }

    default SavedQuery updateSavedQuery(String prevName, SavedQuery newQuery, String username) {
        boilerplate();
        String sql = """
                SELECT * FROM ag_catalog.cypher('dassco'
                   , $$
                       MATCH (u:User {name: $username})<-[:SAVED_BY]-(q:Query {name: $prevName})
                       SET q.name = $newName
                       SET q.query = $newQuery
                       return q.name, q.query
                     $$
                   , #params) as (query_name agtype, query_query agtype);
                """;

        return withHandle(handle -> {
            AgtypeMap params = new AgtypeMapBuilder()
                    .add("username", username)
                    .add("prevName", prevName)
                    .add("newName", newQuery.name)
                    .add("newQuery", newQuery.query).build();
            Agtype agtype = AgtypeFactory.create(params);
            return handle.createQuery(sql)
                    .bind("params", agtype)
                    .map(new SavedQueryMapper())
                    .one();
        });
    }

    default String deleteSavedQuery(String name, String username) {
        boilerplate();
        String sql = """
                SELECT * FROM ag_catalog.cypher('dassco'
                   , $$
                        MATCH (u:User {name: $username})<-[:SAVED_BY]-(q:Query {name: $name})
                        WITH q, q.name AS query_name
                        DETACH DELETE q
                        RETURN query_name
                     $$
                   , #params) as (query_name agtype);
                """;

        return withHandle(handle -> {
            AgtypeMap params = new AgtypeMapBuilder()
                    .add("username", username)
                    .add("name", name).build();
            Agtype agtype = AgtypeFactory.create(params);
            return handle.createQuery(sql)
                    .bind("params", agtype)
                    .mapTo(String.class)
                    .one();
        });
    }


}
