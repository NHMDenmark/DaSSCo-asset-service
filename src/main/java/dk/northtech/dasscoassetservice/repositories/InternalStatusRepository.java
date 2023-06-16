package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.InternalStatus;
import jakarta.inject.Inject;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.core.Jdbi;
import org.postgresql.jdbc.PgConnection;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
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

    public Optional<Map<String, Integer>> getInternalStatusAmt(long currMillisecs) {
        String sql =
                """
                    SELECT * from cypher('dassco', $$
                            MATCH (assets:Asset {internal_status: 'ASSET_RECEIVED'})-[:CHANGED_BY]->(ae:Event {name: 'CREATE_ASSET'})
                            WHERE ae.timestamp >= 1686878830000
                            WITH count(assets) as assetcount
                            OPTIONAL MATCH (completed:Asset {internal_status: 'COMPLETED'})-[:CHANGED_BY]->(ce:Event {name: 'CREATE_ASSET'})
                            WHERE ce.timestamp >= 1686878830000
                            WITH count(completed) as complcount, assetcount
                            OPTIONAL MATCH (metadata:Asset {internal_status: 'METADATA_RECEIVED'})-[:CHANGED_BY]->(me:Event {name: 'CREATE_ASSET'})
                            WHERE me.timestamp >= 1686878830000
                            WITH count(metadata) as metacount, complcount, assetcount
                            RETURN (assetcount + metacount), complcount
                        $$, #params) as (pendingcount agtype, complcount agtype);
               """;


            return jdbi.withHandle(handle -> {
                AgtypeMap today = new AgtypeMapBuilder().add("today", currMillisecs).build();
                Agtype agtype = AgtypeFactory.create(today);
                handle.execute(boilerplate);
                return handle.createQuery(sql)
                        .bind("params", agtype)
                        .map((rs, ctx) -> {
                            Map<String, Integer> amountMap = new HashMap<>();

                            Integer pendingcount = rs.getInt("pendingcount");
                            Integer complcount = rs.getInt("complcount");

                            amountMap.put("pending", pendingcount);
                            amountMap.put("completed", complcount);

                            return amountMap;
                        })
                        .findFirst();
            });
    }
}
