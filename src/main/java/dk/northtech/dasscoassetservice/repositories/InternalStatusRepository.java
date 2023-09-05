package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.InternalStatus;
import jakarta.inject.Inject;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.HashMap;
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
}
