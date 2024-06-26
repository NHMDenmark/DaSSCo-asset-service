package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.AssetGroup;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import dk.northtech.dasscoassetservice.services.AssetGroupService;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.postgresql.jdbc.PgConnection;

import java.sql.Connection;
import java.sql.SQLException;
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
    default Optional<AssetGroup> readAssetGroup(){

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
}
