package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.AssetGroup;
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
    default void createAssetGroup(AssetGroup assetGroup){
        boilerplate();
        createAssetGroupInternal(assetGroup);
    }

    @Transaction
    default Optional<AssetGroup> readAssetGroup(String assetGroupName){
        boilerplate();
        return readAssetGroupInternal(assetGroupName);
    }

    @Transaction
    default List<AssetGroup> readListAssetGroup(boolean roles, Set<String> userRoles){
        boilerplate();
        return readListAssetGroupInternal(roles, userRoles);
    }

    @Transaction
    default void deleteAssetGroup(String groupName){
        boilerplate();
        deleteAssetGroupInternal(groupName);
    }

    @Transaction
    default AssetGroup updateAssetGroup(String assetGroup, List<String> assetList){
        boilerplate();
        updateAssetGroupInternal(assetGroup, assetList);
        Optional<AssetGroup> optionalAssetGroup =  readAssetGroupInternal(assetGroup);
        return optionalAssetGroup.orElseGet(AssetGroup::new);
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

    default void createAssetGroupInternal(AssetGroup assetGroup){

        String assetListAsString = assetGroup.assets.stream()
                .map(asset -> "'" + asset + "'")
                .collect(Collectors.joining(", "));

        String assetGroupSql = """
                SELECT * FROM ag_catalog.cypher(
                    'dassco'
                        , $$
                        MERGE (ag:Asset_Group{name:$group_name})
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

        AgtypeMapBuilder builder = new AgtypeMapBuilder().add("group_name", assetGroup.group_name);

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
                       MATCH (ag:Asset_Group{name:$group_name})-[:CONTAINS]->(a:Asset)
                       RETURN ag.name AS group_name, collect(a.asset_guid) AS asset_guids
                   $$
                , #params) as (group_name agtype, asset_guids agtype);     
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
                        return assetGroup;

                    }).findOne();
        });
    }

    default List<AssetGroup> readListAssetGroupInternal(boolean roles, Set<String> userRoles){

        String userRolesAsString = userRoles.stream()
                .map(role -> {
                    if (role.startsWith("READ_")){
                        role = role.substring(5);
                    } else if (role.startsWith("WRITE_")){
                        role = role.substring(6);
                    }
                    return "'" + role + "'";
                })
                .collect(Collectors.joining(", "));


        String sql = """
                SELECT * FROM ag_catalog.cypher(
                'dassco'
                   , $$
                       MATCH (ag:Asset_Group)-[:CONTAINS]->(a:Asset)
                       RETURN ag.name AS group_name, collect(a.asset_guid) AS asset_guids
                   $$
                ) as (group_name agtype, asset_guids agtype);     
                """;

        String sqlRoles = """
                SELECT * FROM ag_catalog.cypher(
                                'dassco'
                                   , $$
                                       MATCH (ag:Asset_Group)-[:CONTAINS]->(a:Asset)
                                       OPTIONAL MATCH (a)-[:IS_PART_OF]->(c:Collection)-[:RESTRICTED_TO]->(r:Role)
                                       OPTIONAL MATCH (a)-[:BELONGS_TO]->(i:Institution)-[:RESTRICTED_TO]->(r)
                                       WHERE r.name IN [%s]
                                       RETURN ag.name AS group_name, collect(a.asset_guid) AS asset_guids
                                   $$
                                ) as (group_name agtype, asset_guids agtype)
                """.formatted(userRolesAsString);

        if (!roles) {
            return withHandle(handle -> handle.createQuery(sql)
                    .map((rs, ctx) -> {
                        AssetGroup assetGroup = new AssetGroup();
                        Agtype groupName = rs.getObject("group_name", Agtype.class);
                        assetGroup.group_name = groupName.getString();
                        Agtype assets = rs.getObject("asset_guids", Agtype.class);
                        assetGroup.assets = assets.getList().stream().map(x -> String.valueOf(x.toString())).collect(Collectors.toList());
                        return assetGroup;
                    })
                    .list());
        } else {
            return withHandle(handle -> handle.createQuery(sqlRoles)
                    .map((rs, ctx) -> {
                        AssetGroup assetGroup = new AssetGroup();
                        Agtype groupName = rs.getObject("group_name", Agtype.class);
                        assetGroup.group_name = groupName.getString();
                        Agtype assets = rs.getObject("asset_guids", Agtype.class);
                        assetGroup.assets = assets.getList().stream().map(x -> String.valueOf(x.toString())).collect(Collectors.toList());
                        return assetGroup;
                    })
                    .list());
        }
    }

    default void deleteAssetGroupInternal(String groupName){

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

    default void updateAssetGroupInternal(String groupName, List<String> assetList){

        String assetListAsString = assetList.stream()
                .map(asset -> "'" + asset + "'")
                .collect(Collectors.joining(", "));

        // Detach assets, then attach again.
        String detachEdgesSql = """
                SELECT * FROM ag_catalog.cypher(
                'dassco'
                   , $$
                       MATCH (Asset_Group{name:$group_name})-[c:CONTAINS]->(Asset)
                       DELETE c
                   $$
                , #params) as (ag agtype);
                """;

        String attachEdgeSql = """
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

        AgtypeMapBuilder builder = new AgtypeMapBuilder().add("group_name", groupName);

        try {
            withHandle(handle -> {
                Agtype agtype = AgtypeFactory.create(builder.build());
                // Detach Edges (Assets and Asset Group Remain).
                handle.createUpdate(detachEdgesSql)
                        .bind("params", agtype)
                        .execute();
                // Attach Group to Assets
                handle.createUpdate(attachEdgeSql)
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
}
