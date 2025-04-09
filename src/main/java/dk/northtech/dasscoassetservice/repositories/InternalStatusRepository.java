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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    String statusBaseSql = // I know this looks insane but I'm not good enough at neo4j to find a better way yet :C
            """
               SELECT * from cypher('dassco', $$
                    MATCH (a:Asset)-[:CHANGED_BY]->(e:Event {name: 'CREATE_ASSET_METADATA'})
               	 WHERE a.internal_status IN ['ASSET_RECEIVED', 'COMPLETED', 'METADATA_RECEIVED', 'ERDA_ERROR', 'ERDA_FAILED']  #and# 	 \s
               	 OPTIONAL MATCH (a)-[:CHANGED_BY]->(ed:Event {name: 'DELETE_ASSET_METADATA'})
                    WITH
                        count(CASE WHEN a.internal_status IN ['ASSET_RECEIVED', 'METADATA_RECEIVED'] AND ed is null THEN 1 END) as pending,
                        count(CASE WHEN a.internal_status = 'COMPLETED'  AND ed is null THEN 1 END) as completed,
                        count(CASE WHEN a.internal_status IN ['ERDA_ERROR', 'ERDA_FAILED']  AND ed is null THEN 1 END) as failed
                    RETURN
                        pending,
                        completed,
                        failed
               $$, #params) as (pending agtype, completed agtype, failed agtype);
            """;

    String totalAmountSql = statusBaseSql.replaceAll("#and#|, #params", "");
    String dailyAmountSql = statusBaseSql.replace("#and#", Matcher.quoteReplacement("AND e.timestamp >= $today"));

    /**
     * get a map with asset status for current day with keys "failed", "pending" and "completed" and their respective count as values.
     * @param currMillisecs
     * @return
     */

    public Optional<Map<String, Integer>> getDailyInternalStatusAmt(long currMillisecs) {
        return jdbi.withHandle(handle -> {
            AgtypeMap today = new AgtypeMapBuilder().add("today", currMillisecs).build();
            Agtype agtype = AgtypeFactory.create(today);
            handle.execute(boilerplate);
            return handle.createQuery(dailyAmountSql)
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
            return handle.createQuery(totalAmountSql)
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

    String IN_PROGRESS_SQL = """
                SELECT asset_guid
                    , status
                    , error_timestamp
                    , error_message
                    , institution
                    , collection_name AS collection
                    , parent_guid
                FROM asset
                    INNER JOIN collection USING (collection_id)
                WHERE asset.internal_status IN ('METADATA_RECEIVED', 'ERDA_ERROR', 'ASSET_RECEIVED');         
            """;
    public List<AssetStatusInfo> getInprogress() {
        return jdbi.withHandle(handle -> {
            return handle.createQuery(IN_PROGRESS_SQL)
                    .map((rs, ctx) -> {
                        Instant errorTimestamp = null;
                        rs.getString("error_timestamp");
                        if (!rs.wasNull()) {
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
