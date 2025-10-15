package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.SavedQuery;
import dk.northtech.dasscoassetservice.repositories.helpers.AssetMapper;
import dk.northtech.dasscoassetservice.repositories.helpers.SavedQueryMapper;
import org.jdbi.v3.sqlobject.SqlObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface QueriesRepository extends SqlObject {

    /**
     * Retrieve columns for key domain tables.
     */
    default Map<String, List<String>> getNodeProperties() {
        return withHandle(h ->
                h.createQuery("""
                                    SELECT table_name, ARRAY_AGG(column_name) AS column_names
                                    FROM information_schema.columns
                                    WHERE table_name IN ('asset', 'institution', 'collection')
                                    GROUP BY table_name
                                """)
                        .reduceRows(new HashMap<>(), (map, rowView) -> {
                            String table = rowView.getColumn("table_name", String.class);
                            String[] cols = rowView.getColumn("column_names", String[].class);
                            map.put(table, List.of(cols));
                            return map;
                        })
        );
    }

    /**
     * Executes an arbitrary SELECT query returning assets.
     */
    default List<Asset> getAssetsFromQuery(String query) {
        return withHandle(handle -> handle.createQuery(query)
                .map(new AssetMapper())
                .list()
        );
    }

    /**
     * Executes a count(*) query and returns a single integer.
     */
    default int getAssetCountFromQuery(String query) {
        return withHandle(handle ->
                handle.createQuery(query)
                        .mapTo(Integer.class)
                        .one()
        );
    }

    /**
     * Save a new query for a user.
     * If a query with the same name already exists for that user, this fails due to unique constraint.
     */
    default SavedQuery saveQuery(SavedQuery savedQuery, String username) {
        String sql = """
                    INSERT INTO saved_query (name, query, username)
                    VALUES (:name, CAST(:query AS JSONB), :username)
                    RETURNING name, query::text;
                """;

        return withHandle(handle ->
                handle.createQuery(sql)
                        .bind("username", username)
                        .bind("name", savedQuery.name)
                        .bind("query", savedQuery.query)
                        .map(new SavedQueryMapper())
                        .one()
        );
    }

    /**
     * Retrieve all saved queries for a given username.
     */
    default List<SavedQuery> getSavedQueries(String username) {
        String sql = """
                    SELECT name, query::text
                    FROM saved_query
                    WHERE username = :username
                    ORDER BY name;
                """;

        return withHandle(handle ->
                handle.createQuery(sql)
                        .bind("username", username)
                        .map(new SavedQueryMapper())
                        .list()
        );
    }

    /**
     * Update an existing saved query for a user.
     */
    default SavedQuery updateSavedQuery(String name, SavedQuery newQuery, String username) {
        String sql = """
                    UPDATE saved_query
                    SET name = :newName,
                        query = CAST(:newQuery AS JSONB)
                    WHERE name = :oldName AND username = :username
                    RETURNING name, query::text;
                """;

        return withHandle(handle ->
                handle.createQuery(sql)
                        .bind("newName", newQuery.name)
                        .bind("newQuery", newQuery.query)
                        .bind("oldName", name)
                        .bind("username", username)
                        .map(new SavedQueryMapper())
                        .one()
        );
    }

    /**
     * Delete a saved query for a user.
     */
    default String deleteSavedQuery(String name, String username) {
        String sql = """
                    DELETE FROM saved_query
                    WHERE name = :name AND username = :username
                    RETURNING name;
                """;

        return withHandle(handle ->
                handle.createQuery(sql)
                        .bind("name", name)
                        .bind("username", username)
                        .mapTo(String.class)
                        .one()
        );
    }
}