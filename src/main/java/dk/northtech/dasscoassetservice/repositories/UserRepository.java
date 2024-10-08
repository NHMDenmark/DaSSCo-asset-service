package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.postgresql.jdbc.PgConnection;

import java.sql.Connection;
import java.sql.SQLException;

public interface UserRepository extends SqlObject {

    //This must be called once per transaction
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

    @Transaction
    default boolean getUserByUsername(String username){
        boilerplate();
        return this.getUserByUsernameInternal(username);
    }

    @Transaction
    default boolean isUserOwnerOfAssetGroup(String groupName, String username){
        boilerplate();
        return this.isUserOwnerOfAssetGroupInternal(groupName, username);
    }

    @Transaction
    default boolean userHasAccessToAsset(String user, String assetGuid){
        boilerplate();
        return this.userHasAccessToAssetInternal(user, assetGuid);
    }

    default boolean userHasAccessToAssetInternal(String user, String assetGuid){
        String sql = """
                SELECT * FROM ag_catalog.cypher(
                    'dassco'
                        , $$
                        MATCH (u:User {name: $user_name})
                        MATCH (a:Asset {asset_guid: $asset_guid})
                        MATCH (ag:Asset_Group)-[:CONTAINS]->(a)
                        MATCH (u)<-[:HAS_ACCESS]-(ag)
                        RETURN COUNT(*) > 0 AS hasAccess
                    $$
                    , #params) as (hasAccess agtype);
                """;

        AgtypeMapBuilder builder = new AgtypeMapBuilder()
                .add("user_name", user);
        builder.add("asset_guid", assetGuid);

        try {
            return withHandle(handle -> {
                Agtype agtype = AgtypeFactory.create(builder.build());
                return handle.createQuery(sql)
                        .bind("params", agtype)
                        .mapTo(Boolean.class)
                        .findOne()
                        .orElse(false);
            });
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    default boolean getUserByUsernameInternal(String username){
        String sql = """
                SELECT * FROM ag_catalog.cypher(
                    'dassco'
                        , $$
                        MATCH (u:User {name: $user_name})
                        RETURN EXISTS((u))
                    $$
                    , #params) as (u agtype);
                """;

        AgtypeMapBuilder builder = new AgtypeMapBuilder()
                .add("user_name", username);

        try {
            return withHandle(handle -> {
                Agtype agtype = AgtypeFactory.create(builder.build());
                return handle.createQuery(sql)
                        .bind("params", agtype)
                        .mapTo(Boolean.class)
                        .findOne()
                        .orElse(false);
            });
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    default boolean isUserOwnerOfAssetGroupInternal(String groupName, String username){
        String sql = """
                SELECT * FROM ag_catalog.cypher(
                    'dassco'
                        , $$
                        MATCH (u:User {name: $user_name})<-[:MADE_BY]-(ag:Asset_Group {name: $asset_group})
                        RETURN EXISTS((u))
                    $$
                    , #params) as (u agtype);
                """;

        AgtypeMapBuilder builder = new AgtypeMapBuilder()
                .add("user_name", username);
        builder.add("asset_group", groupName);

        try {
            return withHandle(handle -> {
                Agtype agtype = AgtypeFactory.create(builder.build());
                return handle.createQuery(sql)
                        .bind("params", agtype)
                        .mapTo(Boolean.class)
                        .findOne()
                        .orElse(false);
            });
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
