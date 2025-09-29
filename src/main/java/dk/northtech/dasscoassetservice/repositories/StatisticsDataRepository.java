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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(StatisticsDataRepository.class);
    public List<StatisticsData> getGraphData(long startDate, long endDate) {
        logger.info("Start getting statistics from database");
        /*String sql =
            """
SELECT * from cypher('dassco', $$
       MATCH (event:Event {name: 'CREATE_ASSET_METADATA'})<-[:CHANGED_BY]-(asset:Asset)<-[:CREATED_BY]-(specimen:Specimen)
        	WHERE event.timestamp >= $startDate
            	AND event.timestamp <=  $endDate
	   MATCH (pipeline:Pipeline)<-[:USED]-(event)
       MATCH (workstation:Workstation)<-[:USED]-(event)
       MATCH (institution:Institution)<-[:BELONGS_TO]-(asset)
	   OPTIONAL MATCH (asset)-[:CHANGED_BY]->(ed:Event {name: 'DELETE_ASSET_METADATA'})	
       RETURN event.timestamp
		, count(DISTINCT (CASE WHEN ed IS NULL THEN specimen ELSE NULL END))
		, pipeline.name
		, workstation.name
		, institution.name
    $$, #params) as (created_date agtype, specimens agtype, pipeline_name agtype, workstation_name agtype, institute_name agtype)
            """;*/
        String sql = """
                select
                	(EXTRACT(EPOCH FROM e.timestamp) * 1000)::bigint as created_date,
                	COUNT(DISTINCT s.specimen_id) FILTER (WHERE ed IS null) AS specimens,
                	p.pipeline_name,
                	w.workstation_name,
                	c.institution_name
                from specimen s
                inner join asset_specimen as2 on as2.specimen_id = s.specimen_id
                inner join collection c on c.collection_id = s.collection_id
                inner join asset a on a.asset_guid = as2.asset_guid
                inner join workstation w on w.workstation_id = a.workstation_id
                left join event e on e.asset_guid = as2.asset_guid and e.event = 'CREATE_ASSET_METADATA'
                left join event ed on ed.asset_guid = as2.asset_guid and ed.event = 'DELETE_ASSET_METADATA'
                inner join pipeline p on p.pipeline_id = e.pipeline_id
                where e.timestamp::date >= TO_TIMESTAMP(:startDate) AND e.timestamp::date <= TO_TIMESTAMP(:endDate)
                group by e."timestamp", p.pipeline_name, w.workstation_name, c.institution_name
                """;

        List<StatisticsData> graphData = jdbi.withHandle(handle ->
                handle.createQuery(sql)
                .bind("startDate", (startDate/1000))
                .bind("endDate", (endDate/1000))
                .map((rs, ctx) -> new StatisticsData(
                        rs.getString("institution_name"),
                        rs.getString("pipeline_name"),
                        rs.getString("workstation_name"),
                        rs.getLong("created_date"),
                        rs.getInt("specimens")
                ))
                .list());
        logger.info("End getting statistics from database");
        return graphData;
    }

    public List<StatisticsData> testNewSql(long startDate, long endDate) {
        String sql =
            """
                SELECT * FROM ag_catalog.cypher('dassco', $$
                    WITH 86400000 AS oneDayMillis
                    MATCH (event:Event {name: 'CREATE_ASSET_METADATA'})-[:CHANGED_BY]-(asset:Asset)-[:CREATED_BY]-(specimen:Specimen)
                    WHERE NOT EXISTS((:Event {name: 'DELETE_ASSET_METADATA'})-[:CHANGED_BY]-(asset))
                      AND event.timestamp >= $startDate
                      AND event.timestamp <= $endDate
                    WITH event, asset, floor(event.timestamp / oneDayMillis) * oneDayMillis AS dayTimestamp, specimen
                    MATCH (pipeline:Pipeline)<-[:USED]-(event)
                    MATCH (workstation:Workstation)<-[:USED]-(event)
                    MATCH (institution:Institution)-[:BELONGS_TO]-(asset)
                    RETURN dayTimestamp, count(distinct specimen) AS specimenCount, pipeline.name, workstation.name, institution.name
                    ORDER BY dayTimestamp
                         $$, #params) as (created_date agtype, specimens agtype, pipeline_name agtype, workstation_name agtype, institute_name agtype);
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
