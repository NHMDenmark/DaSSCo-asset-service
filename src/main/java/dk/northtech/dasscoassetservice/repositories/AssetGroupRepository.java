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
    default void createAssetGroup(AssetGroup assetGroup, User user){
        boilerplate();
        createAssetGroupInternal(assetGroup, user);
    }

    @Transaction
    default Optional<AssetGroup> readAssetGroup(String assetGroupName){
        boilerplate();
        return readAssetGroupInternal(assetGroupName);
    }

    @Transaction
    default List<AssetGroup> readListAssetGroup(boolean roles, User user){
        boilerplate();
        return readListAssetGroupInternal(roles, user);
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
    default List<String> getHasAccess(String assetGroup){
        boilerplate();
        return getHasAccessInternal(assetGroup);
    }

    default void createAssetGroupInternal(AssetGroup assetGroup, User user){

        String assetListAsString = assetGroup.assets.stream()
                .map(asset -> "'" + asset + "'")
                .collect(Collectors.joining(", "));

        String assetGroupSql = """
                SELECT * FROM ag_catalog.cypher(
                    'dassco'
                        , $$
                        MERGE (u:User {name: $user_name, user_id: $user_name})
                        MERGE (ag:Asset_Group{name:$group_name})
                        MERGE (ag)-[:HAS_ACCESS]-(u)
                        MERGE (ag)-[:MADE_BY]-(u)
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
                       MATCH (u:User)-[:HAS_ACCESS]-(ag:Asset_Group{name:$group_name})-[:CONTAINS]->(a:Asset)
                       RETURN ag.name AS group_name, collect(a.asset_guid) AS asset_guids, u.name
                   $$
                , #params) as (group_name agtype, asset_guids agtype, user_name agtype);     
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
                        assetGroup.hasAccess = new ArrayList<>();
                        assetGroup.hasAccess.add(rs.getObject("user_name", Agtype.class).getString());
                        return assetGroup;
                    }).findOne();
        });
    }

    default List<AssetGroup> readListAssetGroupInternal(boolean roles, User user){

        String sql = """
                SELECT * FROM ag_catalog.cypher(
                'dassco'
                   , $$
                       MATCH (ag:Asset_Group)-[:CONTAINS]->(a:Asset)
                       MATCH (ag)-[:HAS_ACCESS]->(u:User)
                       RETURN ag.name AS group_name, collect(a.asset_guid) AS asset_guids, u.name
                   $$
                ) as (group_name agtype, asset_guids agtype, user_name agtype);     
                """;

        String sqlRoles = """
                SELECT * FROM ag_catalog.cypher(
                'dassco'
                   , $$
                       MATCH (u:User {name: $user_name})<-[:HAS_ACCESS]-(ag:Asset_Group)-[:CONTAINS]->(a:Asset)
                       RETURN ag.name AS group_name, collect(a.asset_guid) AS asset_guids, u.name
                   $$
                , #params) as (group_name agtype, asset_guids agtype, user_name agtype);
                """;

        if (!roles){
            return withHandle(handle -> handle.createQuery(sql)
                    .map((rs, ctx) -> {
                        AssetGroup assetGroup = new AssetGroup();
                        Agtype groupName = rs.getObject("group_name", Agtype.class);
                        assetGroup.group_name = groupName.getString();
                        Agtype assets = rs.getObject("asset_guids", Agtype.class);
                        assetGroup.assets = assets.getList().stream().map(x -> String.valueOf(x.toString())).collect(Collectors.toList());
                        assetGroup.hasAccess = new ArrayList<>();
                        assetGroup.hasAccess.add(rs.getObject("user_name", Agtype.class).getString());
                        return assetGroup;
                    })
                    .list());
        } else {
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
                            assetGroup.hasAccess = new ArrayList<>();
                            assetGroup.hasAccess.add(rs.getObject("user_name", Agtype.class).getString());
                            return assetGroup;
                        })
                        .list();
            });
        }
    }

    default void deleteAssetGroupInternal(String groupName, User user){

        String sql = """
                SELECT * FROM ag_catalog.cypher(
                'dassco'
                   , $$
                       MATCH (ag:Asset_Group{name:$group_name})-[:MADE_BY]->(u:User {name: $user_name})
                       DETACH DELETE ag
                   $$
                , #params) as (ag agtype);
                """;

        AgtypeMapBuilder builder = new AgtypeMapBuilder().add("group_name", groupName);
        builder.add("user_name", user.username);

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
}
