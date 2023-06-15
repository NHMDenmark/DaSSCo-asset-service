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

    public Optional<Map<InternalStatus, Integer>> getInternalStatusAmt(long currMillisecs) {
        String sql =
                """
                    SELECT * from cypher('dassco', $$
                            MATCH (assets:Asset {internal_status: 'ASSET_RECEIVED'})-[:CHANGED_BY]->(ae:Event)
                            WHERE ae.timestamp >= $today
                            WITH count(assets) as assetcount
                            MATCH (completed:Asset {internal_status: 'COMPLETED'})-[:CHANGED_BY]->(ce:Event)
                            WHERE ce.timestamp >= $today
                            WITH count(completed) as complcount, assetcount
                            MATCH (metadata:Asset {internal_status: 'METADATA_RECEIVED'})-[:CHANGED_BY]->(me:Event)
                            WHERE me.timestamp >= $today
                            WITH count(metadata) as metacount, complcount, assetcount
                            RETURN assetcount, complcount, metacount
                        $$, #params) as (assetcount agtype, complcount agtype, metacount agtype);
               """;


            return jdbi.withHandle(handle -> {
                AgtypeMap today = new AgtypeMapBuilder().add("today", currMillisecs).build();
                Agtype agtype = AgtypeFactory.create(today);
                handle.execute(boilerplate);
                return handle.createQuery(sql)
                        .bind("params", agtype)
                        .map((rs, ctx) -> {
                            Map<InternalStatus, Integer> amountMap = new HashMap<>();

                            Integer assetcount = rs.getInt("assetcount");
                            Integer complcount = rs.getInt("complcount");
                            Integer metacount = rs.getInt("metacount");

                            amountMap.put(InternalStatus.METADATA_RECEIVED, metacount);
                            amountMap.put(InternalStatus.COMPLETED, complcount);
                            amountMap.put(InternalStatus.ASSET_RECEIVED, assetcount);

                            return amountMap;
                        })
                        .findFirst();
            });
    }
}
