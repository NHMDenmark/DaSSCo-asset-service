package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.NodeProperty;
import dk.northtech.dasscoassetservice.repositories.helpers.AssetMapper;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import org.apache.age.jdbc.base.Agtype;
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
                    String[] propList = item.properties.split(",");
                    accum.put(item.label.replace("\"", ""), Arrays.asList(propList));
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
}
