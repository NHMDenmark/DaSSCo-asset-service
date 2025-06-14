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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
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
                    , internal_status
                    , error_timestamp
                    , error_message
                    , collection_name AS collection
                FROM asset
                    INNER JOIN collection USING (collection_id)
                WHERE asset.internal_status IN ('METADATA_RECEIVED', 'ERDA_FAILED', 'ASSET_RECEIVED', 'SPECIFY_SYNC_SCHEDULED', 'SPECIFY_SYNC_FAILED', 'SHARE_REOPENED');
            """;
    public List<AssetStatusInfo> getInprogress() {
        return jdbi.withHandle(handle -> {
            return handle.createQuery(IN_PROGRESS_SQL)
                    .map((rs, ctx) -> {
                        Timestamp error_timestamp = rs.getTimestamp("error_timestamp");
                        return new AssetStatusInfo(rs.getString("asset_guid")
                                , null
                                , error_timestamp == null ? null : error_timestamp.toInstant()
                                , InternalStatus.valueOf(rs.getString("internal_status"))
                                , rs.getString("error_message")
                                );
                    })
                    .list();
        });
    }

    String assetStatusSQL =
            """
               SELECT asset_guid
                    , internal_status
                    , error_timestamp
                    , error_message
                    , collection_name AS collection
                FROM asset
                    INNER JOIN collection USING (collection_id)
                WHERE asset.asset_guid = :assetGuid;
                    """;
    public Optional<AssetStatusInfo> getAssetStatus(String assetGuid) {
        return jdbi.withHandle(handle -> {
            AssetRepository assetRepository = handle.attach(AssetRepository.class);
            Set<String> parents = assetRepository.getParents(assetGuid);
            return handle.createQuery(this.assetStatusSQL)
                    .bind("assetGuid", assetGuid)
                    .map((rs, ctx) -> {
                        Timestamp errorTimestamp = rs.getTimestamp("error_timestamp");
                        return new AssetStatusInfo(rs.getString("asset_guid")
                                , parents
                                , errorTimestamp == null ? null : errorTimestamp.toInstant()
                                , InternalStatus.valueOf(rs.getString("internal_status"))
                                , rs.getString("error_message")
                        );
                    })
                    .findOne();
        });
    }
}
