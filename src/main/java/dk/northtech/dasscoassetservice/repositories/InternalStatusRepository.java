package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.AssetStatusInfo;
import dk.northtech.dasscoassetservice.domain.InternalStatus;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;

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

    String statusBaseCypherSql = // I know this looks insane but I'm not good enough at neo4j to find a better way yet :C
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

    String totalAmountCypherSql = statusBaseCypherSql.replaceAll("#and#|, #params", "");
    String dailyAmountCypherSql = statusBaseCypherSql.replace("#and#", Matcher.quoteReplacement("AND e.timestamp >= $today"));

    String statusBaseSql = """
            select internal_status, count(*) from asset
            #where#
            group by internal_status
            order by internal_status ASC
            """;

    String totalAmountSql = statusBaseSql.replaceAll("#where#|, #params", "");
    String dailyAmountSql = statusBaseSql.replace("#where#", Matcher.quoteReplacement("WHERE date_asset_taken::date >= CURRENT_DATE"));
    String customAmountSql = statusBaseSql.replace("#where#", Matcher.quoteReplacement("WHERE date_asset_taken::date >= TO_TIMESTAMP(:startDate) and date_asset_taken::date <= TO_TIMESTAMP(:endDate)"));
//where e.timestamp::date >= TO_TIMESTAMP(:startDate) AND e.timestamp::date <= TO_TIMESTAMP(:endDate)

    /**
     * get a map with asset status for current day with keys "failed", "pending" and "completed" and their respective count as values.
     * @param currMillisecs
     * @return
     */

    public Optional<Map<String, Integer>> getDailyInternalStatusAmt(long currMillisecs) {
        return jdbi.withHandle(handle -> {
            return handle.createQuery(dailyAmountSql)
                    .execute((statement, ctx) -> {
                        try (ctx; var rs = statement.get().getResultSet()) {
                            Map<String, Integer> amountMap = new HashMap<>();
                            while (rs.next()) {
                                String internalStatus = rs.getString("internal_status");
                                Integer internalStatusCount = rs.getInt("count");
                                amountMap.put(internalStatus, internalStatusCount);
                            }
                            return Optional.of(amountMap);
                        }
                    });
        });
    }

    public Optional<Map<String, Integer>> getTotalInternalStatusAmt() {
        return jdbi.withHandle(handle -> {
            return handle.createQuery(totalAmountSql)
                    .execute((statement, ctx) -> {
                        try (ctx; var rs = statement.get().getResultSet()) {
                            Map<String, Integer> amountMap = new HashMap<>();
                            while (rs.next()) {
                                String internalStatus = rs.getString("internal_status");
                                Integer internalStatusCount = rs.getInt("count");
                                amountMap.put(internalStatus, internalStatusCount);
                            }
                            return Optional.of(amountMap);
                        }
                    });
        });
    }

    public Optional<Map<String, Integer>> getDailyInternalStatusAmtCustomRange(long startDate, long endDate) {
        return jdbi.withHandle(handle -> handle.createQuery(customAmountSql)
                .bind("startDate", (startDate/1000))
                .bind("endDate", (endDate/1000))
                .execute((statement, ctx) -> {
                    try (ctx; var rs = statement.get().getResultSet()) {
                        Map<String, Integer> amountMap = new HashMap<>();
                        while (rs.next()) {
                            String internalStatus = rs.getString("internal_status");
                            Integer internalStatusCount = rs.getInt("count");
                            amountMap.put(internalStatus, internalStatusCount);
                        }
                        return Optional.of(amountMap);
                    }
                }));
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
