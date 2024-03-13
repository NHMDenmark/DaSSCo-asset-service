package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.AssetStatusInfo;
import dk.northtech.dasscoassetservice.domain.InternalStatus;
import jakarta.inject.Inject;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class InternalStatusRepository {
    private Jdbi jdbi;
    private DataSource dataSource;

    private static final String boilerplate =
            "CREATE EXTENSION IF NOT EXISTS age;\n" +
            "LOAD 'age';\n" +
            "SET search_path = ag_catalog, \"$user\", public;";

    @Inject
    public InternalStatusRepository(Jdbi jdbi, DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbi = jdbi;
    }

    String totalAmountSql =
            """
                         SELECT * from cypher('dassco', $$
                                 MATCH (assets:Asset {internal_status: 'ASSET_RECEIVED'})-[:CHANGED_BY]->(ae:Event {name: 'CREATE_ASSET'})
                                 WITH count(assets) as assetcount
                                 OPTIONAL MATCH (completed:Asset {internal_status: 'COMPLETED'})-[:CHANGED_BY]->(ce:Event {name: 'CREATE_ASSET'})
                                 WITH count(completed) as complcount, assetcount
                                 OPTIONAL MATCH (metadata:Asset {internal_status: 'METADATA_RECEIVED'})-[:CHANGED_BY]->(me:Event {name: 'CREATE_ASSET'})
                                 WITH count(metadata) as metacount, complcount, assetcount
                                 OPTIONAL MATCH (smberror:Asset {internal_status: 'SMB_ERROR'})-[:CHANGED_BY]->(smbe:Event {name: 'CREATE_ASSET'})
                                 WITH count(smberror) as smbcount, metacount, complcount, assetcount
                                 OPTIONAL MATCH (erdaerror:Asset {internal_status: 'ERDA_ERROR'})-[:CHANGED_BY]->(erde:Event {name: 'CREATE_ASSET'})
                                 WITH count(erdaerror) as erdacount, smbcount, metacount, complcount, assetcount
                                 RETURN complcount, (assetcount + metacount), (erdacount + smbcount)
                             $$) as (completed agtype, pending agtype, failed agtype);
                    """;



    String dailyAmountSql =
            """
                         SELECT * from cypher('dassco', $$
                                 MATCH (assets:Asset {internal_status: 'ASSET_RECEIVED'})-[:CHANGED_BY]->(ae:Event {name: 'CREATE_ASSET'})
                                 WHERE ae.timestamp >= $today
                                 WITH count(assets) as assetcount
                                 OPTIONAL MATCH (completed:Asset {internal_status: 'COMPLETED'})-[:CHANGED_BY]->(ce:Event {name: 'CREATE_ASSET'})
                                 WHERE ce.timestamp >= $today
                                 WITH count(completed) as complcount, assetcount
                                 OPTIONAL MATCH (metadata:Asset {internal_status: 'METADATA_RECEIVED'})-[:CHANGED_BY]->(me:Event {name: 'CREATE_ASSET'})
                                 WHERE me.timestamp >= $today
                                 WITH count(metadata) as metacount, complcount, assetcount
                                 OPTIONAL MATCH (smberror:Asset {internal_status: 'SMB_ERROR'})-[:CHANGED_BY]->(smbe:Event {name: 'CREATE_ASSET'})
                                 WHERE smbe.timestamp >= $today
                                 WITH count(smberror) as smbcount, metacount, complcount, assetcount
                                 OPTIONAL MATCH (erdaerror:Asset {internal_status: 'ERDA_ERROR'})-[:CHANGED_BY]->(erde:Event {name: 'CREATE_ASSET'})
                                 WHERE erde.timestamp >= $today
                                 WITH count(erdaerror) as erdacount, smbcount, metacount, complcount, assetcount
                                 RETURN complcount, (assetcount + metacount), (erdacount + smbcount)
                             $$, #params) as (completed agtype, pending agtype, failed agtype);
                    """;

    public Optional<Map<String, Integer>> getDailyInternalStatusAmt(long currMillisecs) {
        return jdbi.withHandle(handle -> {
            AgtypeMap today = new AgtypeMapBuilder().add("today", currMillisecs).build();
            Agtype agtype = AgtypeFactory.create(today);
            handle.execute(boilerplate);
            return handle.createQuery(this.dailyAmountSql)
                    .bind("params", agtype)
                    .map((rs, ctx) -> {
                        Map<String, Integer> amountMap = new HashMap<>();

                        Integer failedcount = rs.getInt("failed");
                        Integer pendingcount = rs.getInt("pending");
                        Integer complcount = rs.getInt("completed");

                        amountMap.put("failed", failedcount);
                        amountMap.put("pending", pendingcount);
                        amountMap.put("completed", complcount);

                        return amountMap;
                    })
                    .findFirst();
        });
    }

    public Optional<Map<String, Integer>> getTotalInternalStatusAmt() {
        return jdbi.withHandle(handle -> {
            handle.execute(boilerplate);
            return handle.createQuery(this.totalAmountSql)
                    .map((rs, ctx) -> {
                        Map<String, Integer> amountMap = new HashMap<>();

                        Integer failedcount = rs.getInt("failed");
                        Integer pendingcount = rs.getInt("pending");
                        Integer complcount = rs.getInt("completed");

                        amountMap.put("failed", failedcount);
                        amountMap.put("pending", pendingcount);
                        amountMap.put("completed", complcount);

                        return amountMap;
                    })
                    .findFirst();
        });
    }

    String assetErrorSQL =
            """
                         SELECT * FROM cypher('dassco', $$
                                                          MATCH (a:Asset {internal_status: 'ASSET_RECEIVED'})
                                                          MATCH (i:Institution)<-[:BELONGS_TO]-(a)
                                                          MATCH (c:Collection)<-[:IS_PART_OF]-(a)
                                                          OPTIONAL MATCH (a)-[:CHILD_OF]->(pa:Asset)
                                                          return a.asset_guid, a.internal_status, a.error_timestamp,a.error_message, i.name, c.name , pa.asset_guid
                                                      $$) as (asset_guid text, status text, error_timestamp agtype, error_message text, institution text, collection text, parent_guid text)
                                  UNION ALL
                                  SELECT * FROM cypher('dassco', $$
                                                          MATCH (a:Asset {internal_status: 'ERDA_ERROR'})
                                                          MATCH (i:Institution)<-[:BELONGS_TO]-(a)
                                                          MATCH (c:Collection)<-[:IS_PART_OF]-(a)
                                                          OPTIONAL MATCH (a)-[:CHILD_OF]->(pa:Asset)
                                                          return a.asset_guid, a.internal_status, a.error_timestamp,a.error_message, i.name, c.name, pa.asset_guid
                                                      $$) as (asset_guid text, status text, error_timestamp agtype, error_message text, institution text, collection text, parent_guid text)
                                  UNION ALL
                                  SELECT * FROM cypher('dassco', $$
                                                          MATCH (a:Asset {internal_status: 'METADATA_RECEIVED'})
                                                          MATCH (i:Institution)<-[:BELONGS_TO]-(a)
                                                          MATCH (c:Collection)<-[:IS_PART_OF]-(a)
                                                          OPTIONAL MATCH (a)-[:CHILD_OF]->(pa:Asset)
                                                          return a.asset_guid, a.internal_status, a.error_timestamp, a.error_message, i.name, c.name, pa.asset_guid
                                                      $$) as (asset_guid text, status text, error_timestamp agtype, error_message text, institution text, collection text, parent_guid text)                                                        
                                                      ;
                    """;
    public List<AssetStatusInfo> getInprogress() {
        return jdbi.withHandle(handle -> {
            handle.execute(boilerplate);
            return handle.createQuery(this.assetErrorSQL)
                    .map((rs, ctx) -> {
                        Instant errorTimestamp = null;
                        rs.getString("error_timestamp");
                        if (!rs.wasNull()) {
//                            Agtype dateAssetFinalised = rs.getObject("error_timestamp", Agtype.class);
                            errorTimestamp = Instant.ofEpochMilli(rs.getLong("error_timestamp"));
                        }
                        return new AssetStatusInfo(rs.getString("asset_guid")
                                , rs.getString("parent_guid")
                                , errorTimestamp
                                , InternalStatus.valueOf(rs.getString("status"))
                                , rs.getString("error_message")
                                );
                    })
                    .list();
        });
    }

    String assetStatusSQL =
            """
                         SELECT * FROM cypher('dassco', $$
                                                          MATCH (a:Asset {name: $asset_guid})
                                                          MATCH (i:Institution)<-[:BELONGS_TO]-(a)
                                                          MATCH (c:Collection)<-[:IS_PART_OF]-(a)
                                                          OPTIONAL MATCH (a)-[:CHILD_OF]->(pa:Asset)
                                                          return a.asset_guid, a.internal_status, a.error_timestamp,a.error_message, i.name, c.name , pa.asset_guid
                                                      $$, #params) as (asset_guid text, status text, error_timestamp agtype, error_message text, institution text, collection text, parent_guid text)
                                                              
                                                      ;
                    """;
    public Optional<AssetStatusInfo> getAssetStatus(String assetGuid) {
        return jdbi.withHandle(handle -> {
            handle.execute(boilerplate);
            AgtypeMap agParams = new AgtypeMapBuilder()
                    .add("asset_guid", assetGuid).build();
            Agtype agtypeParams = AgtypeFactory.create(agParams);
            return handle.createQuery(this.assetStatusSQL)
                    .bind("params", agtypeParams)
                    .map((rs, ctx) -> {
                        Instant errorTimestamp = null;
                        rs.getString("error_timestamp");
                        if (!rs.wasNull()) {
//                            Agtype dateAssetFinalised = rs.getObject("error_timestamp", Agtype.class);
                            errorTimestamp = Instant.ofEpochMilli(rs.getLong("error_timestamp"));
                        }
                        return new AssetStatusInfo(rs.getString("asset_guid")
                                , rs.getString("parent_guid")
                                , errorTimestamp
                                , InternalStatus.valueOf(rs.getString("status"))
                                , rs.getString("error_message")
                        );
                    })
                    .findOne();
        });
    }
}
