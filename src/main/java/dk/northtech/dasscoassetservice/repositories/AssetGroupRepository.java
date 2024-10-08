package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.AssetGroup;
import dk.northtech.dasscoassetservice.domain.User;
import dk.northtech.dasscoassetservice.repositories.helpers.AssetMapper;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import dk.northtech.dasscoassetservice.services.AssetGroupService;
import dk.northtech.dasscoassetservice.webapi.v1.AssetGroups;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.checkerframework.checker.units.qual.A;
import org.jdbi.v3.sqlobject.CreateSqlObject;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.postgresql.jdbc.PgConnection;

import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public interface AssetGroupRepository extends SqlObject {

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
    default void createAssetGroup(AssetGroup assetGroup, User user, Instant now){
        boilerplate();
        createAssetGroupInternal(assetGroup, user, now);
    }

    @Transaction
    default Optional<AssetGroup> readAssetGroup(String assetGroupName){
        boilerplate();
        return readAssetGroupInternal(assetGroupName);
    }

    @Transaction
    default List<AssetGroup> readListAssetGroup(User user){
        boilerplate();
        return readListAssetGroupInternal(user);
    }

    @Transaction
    default List<AssetGroup> readOwnedListAssetGroup(User user){
        boilerplate();
        return readOwnedListAssetGroupInternal(user);
    }

    @Transaction
    default void deleteAssetGroup(String groupName, User user){
        boilerplate();
        deleteAssetGroupInternal(groupName, user);
    }

    @Transaction
    default Optional<AssetGroup> addAssetsToAssetGroup(List<String> assetList, String groupName){
        boilerplate();
        return addAssetsToAssetGroupInternal(assetList, groupName);
    }

    @Transaction
    default Optional<AssetGroup> removeAssetsFromAssetGroup(List<String> assetList, String groupName){
        boilerplate();
        return removeAssetsFromAssetGroupInternal(assetList, groupName);
    }

    @Transaction
    default Optional<AssetGroup> grantAccessToAssetGroup(List<String> users, String groupName){
        boilerplate();
        return grantAccessToAssetGroupInternal(users, groupName);
    }

    @Transaction
    default Optional<AssetGroup> revokeAccessToAssetGroup(List<String> users, String groupName){
        boilerplate();
        return revokeAccessToAssetGroupInternal(users, groupName);
    }

    @Transaction
    default List<String> getHasAccess(String assetGroup){
        boilerplate();
        return getHasAccessInternal(assetGroup);
    }

    default void createAssetGroupInternal(AssetGroup assetGroup, User user, Instant now){

        String assetListAsString = assetGroup.assets.stream()
                .map(asset -> "'" + asset + "'")
                .collect(Collectors.joining(", "));

        String assetGroupSql = """
                SELECT * FROM ag_catalog.cypher(
                    'dassco'
                        , $$
                        MERGE (u:User {name: $user_name, user_id: $user_name})
                        MERGE (ag:Asset_Group{name:$group_name, timestamp:$timestamp})
                        MERGE (ag)-[:HAS_ACCESS]->(u)
                        MERGE (ag)-[:MADE_BY]->(u)
                    $$
                    , #params) as (a agtype);
                """;

        String sql = """
                SELECT * FROM ag_catalog.cypher(
                    'dassco'
                        , $$
                            MATCH (ag:Asset_Group{name:$group_name})
                            MATCH (a:Asset)
                            
                            WHERE a.asset_guid IN [%s]
                            MERGE (ag)-[:CONTAINS]->(a)
                        $$
                    , #params) as (a agtype);
                """.formatted(assetListAsString);

        AgtypeMapBuilder builder = new AgtypeMapBuilder()
                .add("group_name", assetGroup.group_name);
                builder.add("user_name", user.username);
                builder.add("timestamp", now.toEpochMilli());

        try {
            withHandle(handle -> {
                Agtype agtype = AgtypeFactory.create(builder.build());
                handle.createUpdate(assetGroupSql)
                        .bind("params", agtype)
                        .execute();
                handle.createUpdate(sql)
                        .bind("params", agtype)
                        .execute();
                return handle;
            });
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    default Optional<AssetGroup> readAssetGroupInternal(String assetGroupName){
        String sql = """
                SELECT * FROM ag_catalog.cypher(
                'dassco'
                   , $$
                       MATCH (u:User)-[:HAS_ACCESS]-(ag:Asset_Group {name: $group_name})
                       MATCH (ag)-[:CONTAINS]->(a:Asset)
                       MATCH (ag)-[:MADE_BY]->(uu:User)
                       RETURN ag.name AS group_name, collect(DISTINCT a.asset_guid) AS asset_guids, collect(DISTINCT u.name) AS user_names, uu.name
                   $$
                , #params) as (group_name agtype, asset_guids agtype, user_name agtype, group_creator agtype);     
                """;

        return withHandle(handle -> {
            AgtypeMap agParams = new AgtypeMapBuilder()
                    .add("group_name", assetGroupName)
                    .build();
            Agtype agtype = AgtypeFactory.create(agParams);
            return handle.createQuery(sql)
                    .bind("params", agtype)
                    .map((rs, ctx) -> {

                        AssetGroup assetGroup = new AssetGroup();
                        Agtype groupName = rs.getObject("group_name", Agtype.class);
                        assetGroup.group_name = groupName.getString();
                        Agtype assets = rs.getObject("asset_guids", Agtype.class);
                        assetGroup.assets = assets.getList().stream().map(x -> String.valueOf(x.toString())).collect(Collectors.toList());
                        Agtype hasAccess = rs.getObject("user_name", Agtype.class);
                        assetGroup.hasAccess = hasAccess.getList().stream().map(x -> String.valueOf(x.toString())).collect(Collectors.toList());
                        Agtype groupCreator = rs.getObject("group_creator", Agtype.class);
                        assetGroup.groupCreator = groupCreator.getString();
                        return assetGroup;
                    }).findOne();
        });
    }

    default List<AssetGroup> readListAssetGroupInternal(User user){

        String sqlRoles = """
                SELECT * FROM ag_catalog.cypher(
                'dassco'
                   , $$
                       MATCH (u:User {name: $user_name})<-[:HAS_ACCESS]-(ag:Asset_Group)-[:CONTAINS]->(a:Asset)
                       WITH ag, a, u
                       MATCH (ag)-[:HAS_ACCESS]->(allUsers:User)
                       MATCH (ag)-[:MADE_BY]->(uu:User)
                       RETURN ag.name AS group_name, collect(DISTINCT a.asset_guid) AS asset_guids, collect(DISTINCT allUsers.name) AS user_names, uu.name
                   $$
                , #params) as (group_name agtype, asset_guids agtype, user_name agtype, group_creator agtype);
                """;

            return withHandle(handle -> {
                AgtypeMap agParams = new AgtypeMapBuilder()
                        .add("user_name", user.username)
                        .build();
                Agtype agtype = AgtypeFactory.create(agParams);
                return handle.createQuery(sqlRoles)
                        .bind("params", agtype)
                        .map((rs, ctx) -> {
                            AssetGroup assetGroup = new AssetGroup();
                            Agtype groupName = rs.getObject("group_name", Agtype.class);
                            assetGroup.group_name = groupName.getString();
                            Agtype assets = rs.getObject("asset_guids", Agtype.class);
                            assetGroup.assets = assets.getList().stream().map(x -> String.valueOf(x.toString())).collect(Collectors.toList());
                            Agtype users = rs.getObject("user_name", Agtype.class);
                            assetGroup.hasAccess = users.getList().stream().map(x -> String.valueOf(x.toString())).collect(Collectors.toList());
                            Agtype groupCreator = rs.getObject("group_creator", Agtype.class);
                            assetGroup.groupCreator = groupCreator.getString();
                            return assetGroup;
                        })
                        .list();
            });
    }

    default List<AssetGroup> readOwnedListAssetGroupInternal(User user){

        String sqlRoles = """
                SELECT * FROM ag_catalog.cypher(
                'dassco'
                   , $$
                       MATCH (ag:Asset_Group)-[:CONTAINS]->(a:Asset)
                       MATCH (ag)-[:HAS_ACCESS]->(u:User)
                       MATCH (ag)-[:MADE_BY]->(uu:User {name: $username})
                       RETURN ag.name AS group_name, collect(DISTINCT a.asset_guid) AS asset_guids, collect(DISTINCT u.name) as user_names, uu.name
                   $$
                , #params) as (group_name agtype, asset_guids agtype, user_name agtype, group_creator agtype);
                """;

            return withHandle(handle -> {
                AgtypeMap agParams = new AgtypeMapBuilder()
                        .add("username", user.username)
                        .build();
                Agtype agtype = AgtypeFactory.create(agParams);
                return handle.createQuery(sqlRoles)
                        .bind("params", agtype)
                        .map((rs, ctx) -> {
                            AssetGroup assetGroup = new AssetGroup();
                            Agtype groupName = rs.getObject("group_name", Agtype.class);
                            assetGroup.group_name = groupName.getString();
                            Agtype assets = rs.getObject("asset_guids", Agtype.class);
                            assetGroup.assets = assets.getList().stream().map(x -> String.valueOf(x.toString())).collect(Collectors.toList());
                            Agtype users = rs.getObject("user_name", Agtype.class);
                            assetGroup.hasAccess = users.getList().stream().map(x -> String.valueOf(x.toString())).collect(Collectors.toList());
                            Agtype groupCreator = rs.getObject("group_creator", Agtype.class);
                            assetGroup.groupCreator = groupCreator.getString();
                            return assetGroup;
                        })
                        .list();
            });
    }

    default void deleteAssetGroupInternal(String groupName, User user){

        String sql = """
                SELECT * FROM ag_catalog.cypher(
                'dassco'
                   , $$
                       MATCH (ag:Asset_Group{name:$group_name})
                       DETACH DELETE ag
                   $$
                , #params) as (ag agtype);
                """;

        AgtypeMapBuilder builder = new AgtypeMapBuilder().add("group_name", groupName);

        try {
            withHandle(handle -> {
                Agtype agtype = AgtypeFactory.create(builder.build());
                handle.createUpdate(sql)
                        .bind("params", agtype)
                        .execute();
                return handle;
            });
        } catch (Exception e){
            throw new RuntimeException(e);
        }

    }

    default Optional<AssetGroup> addAssetsToAssetGroupInternal(List<String> assetList, String groupName){

        String assetListAsString = assetList.stream()
                .map(asset -> "'" + asset + "'")
                .collect(Collectors.joining(", "));

        String sql = """
                SELECT * FROM ag_catalog.cypher('dassco', $$
                MATCH (ag:Asset_Group {name: $group_name})
                MATCH (a:Asset)
                WHERE a.name IN [%s]
                MERGE (ag)-[:CONTAINS]->(a)
                RETURN ag.name AS group_name
                $$, #params) as (group_name agtype);
                """.formatted(assetListAsString);

        AgtypeMapBuilder builder = new AgtypeMapBuilder().add("group_name", groupName);

        try {
            withHandle(handle -> {
                Agtype agtype = AgtypeFactory.create(builder.build());
                return handle.createUpdate(sql)
                        .bind("params", agtype)
                        .execute();
                        });
        } catch (Exception e){
            throw new RuntimeException(e);
        }
        return this.readAssetGroupInternal(groupName);
    }

    default Optional<AssetGroup> removeAssetsFromAssetGroupInternal(List<String> assetList, String groupName){
        String assetListAsString = assetList.stream()
                .map(asset -> "'" + asset + "'")
                .collect(Collectors.joining(", "));

        String sql = """
                SELECT * FROM ag_catalog.cypher('dassco', $$
                MATCH (ag:Asset_Group {name: $group_name})
                MATCH (a:Asset)
                WHERE a.name IN [%s]
                MATCH (ag)-[r:CONTAINS]->(a)
                DELETE r
                RETURN ag
                $$, #params) as (ag agtype);
                """.formatted(assetListAsString);

        AgtypeMapBuilder builder = new AgtypeMapBuilder().add("group_name", groupName);

        try {
            withHandle(handle -> {
                Agtype agtype = AgtypeFactory.create(builder.build());
                handle.createUpdate(sql)
                        .bind("params", agtype)
                        .execute();

                return handle;
            });
        } catch (Exception e){
            throw new RuntimeException(e);
        }

        return readAssetGroupInternal(groupName);

    }

    default List<String> getHasAccessInternal(String groupName) {
        String sql = """
                SELECT * FROM ag_catalog.cypher('dassco', $$
                MATCH (u: User)<-[:HAS_ACCESS]-(ag:Asset_Group {name: $group_name})
                return u.name
                $$, #params) as (users agtype);
                """;

        AgtypeMapBuilder builder = new AgtypeMapBuilder().add("group_name", groupName);

        try {
            return withHandle(handle -> {
                Agtype agtype = AgtypeFactory.create(builder.build());
                return handle.createQuery(sql)
                        .bind("params", agtype)
                        .map((rs, ctx) -> rs.getString("users").replace("\"", "")).list();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    default Optional<AssetGroup> grantAccessToAssetGroupInternal(List<String> users, String groupName){

        String userListAsString = users.stream()
                .map(asset -> "'" + asset + "'")
                .collect(Collectors.joining(", "));

        String sql = """
                SELECT * FROM ag_catalog.cypher('dassco', $$
                MATCH (u: User)
                WHERE u.name IN [%s]
                MATCH (ag:Asset_Group { name: $group_name })
                MERGE (u)<-[:HAS_ACCESS]-(ag)
                return ag
                $$, #params) as (ag agtype);
                """.formatted(userListAsString);

        AgtypeMapBuilder builder = new AgtypeMapBuilder().add("group_name", groupName);

        try {
            withHandle(handle -> {
                Agtype agtype = AgtypeFactory.create(builder.build());
                handle.createUpdate(sql)
                        .bind("params", agtype)
                        .execute();

                return handle;
            });
        } catch (Exception e){
            throw new RuntimeException(e);
        }

        return readAssetGroupInternal(groupName);
    }

    default Optional<AssetGroup> revokeAccessToAssetGroupInternal(List<String> users, String groupName){
        String userListAsString = users.stream()
                .map(asset -> "'" + asset + "'")
                .collect(Collectors.joining(", "));

        String sql = """
                SELECT * FROM ag_catalog.cypher('dassco', $$
                MATCH (u: User)
                WHERE u.name IN [%s]
                MATCH (ag:Asset_Group { name: $group_name })
                MATCH (u)<-[r:HAS_ACCESS]-(ag)
                DELETE r
                $$, #params) as (ag agtype);
                """.formatted(userListAsString);

        AgtypeMapBuilder builder = new AgtypeMapBuilder().add("group_name", groupName);

        try {
            withHandle(handle -> {
                Agtype agtype = AgtypeFactory.create(builder.build());
                handle.createUpdate(sql)
                        .bind("params", agtype)
                        .execute();

                return handle;
            });
        } catch (Exception e){
            throw new RuntimeException(e);
        }
        return readAssetGroupInternal(groupName);
    }
}
