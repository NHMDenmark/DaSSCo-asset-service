package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.InternalStatus;
import dk.northtech.dasscoassetservice.domain.StatisticsData;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import jakarta.inject.Inject;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.statement.HashPrefixSqlParser;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class StatisticsDataRepository {
    private Jdbi jdbi;

    @Inject
    public StatisticsDataRepository(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public List<StatisticsData> getGraphData(long startDate, long endDate) {
        String sql =
            """
                SELECT * from cypher('dassco', $$
                        MATCH (event:Event {name: 'CREATE_ASSET_METADATA'})-[:CHANGED_BY]-(asset)-[:CREATED_BY]-(specimen)
                        MATCH (pipeline:Pipeline)<-[:USED]-(event)
                        MATCH (workstation:Workstation)<-[:USED]-(event)
                        MATCH (institution)-[:BELONGS_TO]-(asset)
                        WHERE event.timestamp >= $startDate AND event.timestamp <= $endDate
                        RETURN event.timestamp, count(specimen), pipeline.name, workstation.name, institution.name
                    $$, #params) as (created_date agtype, specimens agtype, pipeline_name agtype, workstation_name agtype, institute_name agtype)
            """;

        return jdbi.withHandle(handle -> {
            AgtypeMap dateParam = new AgtypeMapBuilder()
                    .add("startDate", startDate)
                    .add("endDate", endDate)
                    .build();
            Agtype agtype = AgtypeFactory.create(dateParam);
            handle.execute(DBConstants.AGE_BOILERPLATE);
            return handle.createQuery(sql)
                .bind("params", agtype)
                .map((rs, ctx) -> new StatisticsData(
                        rs.getString("institute_name"),
                        rs.getString("pipeline_name"),
                        rs.getString("workstation_name"),
                        rs.getLong("created_date"),
                        rs.getInt("specimens")
                ))
                .list();
        });
    }
}
