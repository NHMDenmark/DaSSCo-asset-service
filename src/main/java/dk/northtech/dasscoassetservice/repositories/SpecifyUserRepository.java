package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Pipeline;
import dk.northtech.dasscoassetservice.domain.SpecifyUser;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.sqlobject.SqlObject;
import org.postgresql.jdbc.PgConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface SpecifyUserRepository extends SqlObject {

    default void boilerplate() {
        withHandle(handle -> {
            Connection connection = handle.getConnection();
            try {
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                handle.execute("set search_path TO ag_catalog;");
                return handle;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    default Optional<SpecifyUser> createSpecifyUser(SpecifyUser specifyUser) {
        boilerplate();
        String sql =
                """
                    SELECT * FROM ag_catalog.cypher('dassco', $$
                        MATCH (i:Institution {name: $institution})
                        MERGE (s:Specify_User {name: $username, url: $url})
                        MERGE (s)-[:BELONGS_TO]->(i)
                        RETURN s.name, s.url, i.name
                         $$
                        , #params
                  ) as (name agtype, url agtype, institution agtype);
                """;

        return withHandle(handle -> {
            AgtypeMap agParams = new AgtypeMapBuilder()
                    .add("institution", specifyUser.institution())
                    .add("username", specifyUser.username())
                    .add("url", specifyUser.url())
                    .build();
            Agtype agtype = AgtypeFactory.create(agParams);
            return handle.createQuery(sql)
                .bind("params", agtype)
                .map((rs, ctx) -> {
                    Agtype username = rs.getObject("name", Agtype.class);
                    Agtype url = rs.getObject("url", Agtype.class);
                    Agtype institution = rs.getObject("institution", Agtype.class);
                    return new SpecifyUser(username.getString(), url.getString(), institution.getString());
                }).findOne();
        });
    }

    default Optional<SpecifyUser> getSpecifyUserFromInstitution(String institutionName) {
        boilerplate();
        String sql =
            """
                SELECT * FROM ag_catalog.cypher('dassco', $$
                    OPTIONAL MATCH (s:Specify_User)-[:BELONGS_TO]->(i:Institution {name: $institutionName})
                    RETURN s.name, s.url, i.name
                     $$
                    , #params
              ) as (name agtype, url agtype, institution agtype);
            """;

        return withHandle(handle -> {
            AgtypeMap agParams = new AgtypeMapBuilder()
                    .add("institutionName", institutionName)
                    .build();
            Agtype agtype = AgtypeFactory.create(agParams);
            return handle.createQuery(sql)
                .bind("params", agtype)
                .map((rs, ctx) -> {
                    Agtype username = rs.getObject("name", Agtype.class);
                    Agtype url = rs.getObject("url", Agtype.class);
                    Agtype institution = rs.getObject("institution", Agtype.class);
                    if (username.isNull() && url.isNull() && institution.isNull()) return null;
                    return new SpecifyUser(username.getString(), url.getString(), institution.getString());
                }).findOne();
        });
    }

    default List<SpecifyUser> getAllSpecifyUsers() {
        boilerplate();
        String sql =
            """
                SELECT * FROM ag_catalog.cypher('dassco', $$
                    MATCH (s:Specify_User)-[:BELONGS_TO]->(i:Institution)
                    RETURN s.name, s.url, i.name
                     $$
              ) as (name agtype, url agtype, institution agtype);
            """;

        return withHandle(handle -> handle.createQuery(sql)
                .map((rs, ctx) -> {
                    Agtype username = rs.getObject("name", Agtype.class);
                    Agtype url = rs.getObject("url", Agtype.class);
                    Agtype institution = rs.getObject("institution", Agtype.class);
                    return new SpecifyUser(username.getString(), url.getString(), institution.getString());
                })).list();
    }

    default Optional<SpecifyUser> getSpecifyUserFromUsername(String username) {
        boilerplate();
        String sql =
            """
                SELECT * FROM ag_catalog.cypher('dassco', $$
                    MATCH (s:Specify_User {name: $username})-[:BELONGS_TO]->(i:Institution)
                    RETURN s.name, s.url, i.name
                     $$
                    , #params
              ) as (name agtype, url agtype, institution agtype);
            """;

        return withHandle(handle -> {
            AgtypeMap agParams = new AgtypeMapBuilder()
                    .add("username", username)
                    .build();
            Agtype agtype = AgtypeFactory.create(agParams);
            return handle.createQuery(sql)
                .bind("params", agtype)
                .map((rs, ctx) -> {
                    Agtype username_result = rs.getObject("name", Agtype.class);
                    Agtype url = rs.getObject("url", Agtype.class);
                    Agtype institution = rs.getObject("institution", Agtype.class);
                    return new SpecifyUser(username_result.getString(), url.getString(), institution.getString());
                }).findOne();
        });
    }

    default Optional<SpecifyUser> updateSpecifyUser(String institutionName, SpecifyUser specifyUser) {
        boilerplate();
        String sql =
            """
                SELECT * FROM ag_catalog.cypher('dassco', $$
                    MATCH (s:Specify_User)-[:BELONGS_TO]->(i:Institution {name: $institutionName})
                    SET s.name = $newUsername
                    SET s.url = $newUrl
                    RETURN s.name, s.url, i.name
                     $$
                    , #params
              ) as (name agtype, url agtype, institution agtype);
            """;

        return withHandle(handle -> {
            AgtypeMap agParams = new AgtypeMapBuilder()
                    .add("institutionName", institutionName)
                    .add("newUsername", specifyUser.username())
                    .add("newUrl", specifyUser.url())
                    .build();
            Agtype agtype = AgtypeFactory.create(agParams);
            return handle.createQuery(sql)
                    .bind("params", agtype)
                    .map((rs, ctx) -> {
                        Agtype username = rs.getObject("name", Agtype.class);
                        Agtype url = rs.getObject("url", Agtype.class);
                        Agtype institution = rs.getObject("institution", Agtype.class);
                        return new SpecifyUser(username.getString(), url.getString(), institution.getString());
                    }).findOne();
        });
    }

    default Optional<SpecifyUser> deleteSpecifyUser(String institutionName) {
        boilerplate();
        String sql =
            """
                SELECT * FROM ag_catalog.cypher('dassco', $$
                    MATCH (s:Specify_User)-[:BELONGS_TO]->(i:Institution {name: $institutionName})
                    DETACH DELETE s
                    RETURN s.name, s.url, i.name
                     $$
                    , #params
              ) as (name agtype, url agtype, institution agtype);
            """;

        return withHandle(handle -> {
            AgtypeMap agParams = new AgtypeMapBuilder()
                    .add("institutionName", institutionName)
                    .build();
            Agtype agtype = AgtypeFactory.create(agParams);
            return handle.createQuery(sql)
                    .bind("params", agtype)
                    .map((rs, ctx) -> {
                        Agtype username = rs.getObject("name", Agtype.class);
                        Agtype url = rs.getObject("url", Agtype.class);
                        Agtype institution = rs.getObject("institution", Agtype.class);
                        return new SpecifyUser(username.getString(), url.getString(), institution.getString());
                    }).findOne();
        });
    }
}
