package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.AssetGroup;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import dk.northtech.dasscoassetservice.services.AssetGroupService;
import dk.northtech.dasscoassetservice.webapi.v1.AssetGroups;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.checkerframework.checker.units.qual.A;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.postgresql.jdbc.PgConnection;

import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
}
