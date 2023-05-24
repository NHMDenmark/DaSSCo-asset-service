package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.StatisticsData;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.statement.HashPrefixSqlParser;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;

@Repository
public class StatisticsDataRepository {
    private Jdbi jdbi;
    private DataSource dataSource;

    @Inject
    public StatisticsDataRepository(Jdbi jdbi, DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbi = Jdbi.create(dataSource)
                .registerRowMapper((ConstructorMapper.factory(StatisticsData.class)));
    }

    public List<StatisticsData> getGraphData(long startDate, long endDate) {
        String sql =
            """
                WITH statistics_data as (
                    SELECT * from cypher('dassco', $$
                        MATCH (event)-[:CHANGED_BY]-(asset)-[:CREATED_BY]-(specimen)
                        OPTIONAL MATCH (pipeline:Pipeline)<-[:USED]-(event)
                        OPTIONAL MATCH (workstation:Workstation)<-[:USED]-(event)
                        OPTIONAL MATCH (institution)-[:BELONGS_TO]-(asset)
                        RETURN event.timestamp, count(specimen), pipeline.name, workstation.name, institution.name
                    $$) as (created_date agtype, specimens agtype, pipeline_name agtype, workstation_name agtype, institute_name agtype))
                SELECT * FROM statistics_data
                    WHERE to_timestamp(agtype_to_float8(statistics_data.created_date / 1000)) >= to_timestamp(? / 1000)
                      AND to_timestamp(agtype_to_float8(statistics_data.created_date / 1000)) <= to_timestamp(? / 1000);
            """;

        return jdbi.withHandle(handle -> {
            handle.execute(DBConstants.AGE_BOILERPLATE);
            return handle.setSqlParser(new HashPrefixSqlParser())
                .createQuery(sql)
                .bind(0, startDate)
                .bind(1, endDate)
                .mapTo(StatisticsData.class)
                .list();
        });
    }
}