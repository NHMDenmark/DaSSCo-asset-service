package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.GraphData;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import jakarta.inject.Inject;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.statement.HashPrefixSqlParser;
import org.postgresql.jdbc.PgConnection;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class GraphDataRepository {
    private Jdbi jdbi;
    private DataSource dataSource;

    @Inject
    public GraphDataRepository(Jdbi jdbi, DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbi = Jdbi.create(dataSource)
                .registerRowMapper((ConstructorMapper.factory(GraphData.class)));
    }


    public List<GraphData> getGraphData() {
        String sql =
            """
                SELECT * from cypher('dassco', $$
                    MATCH (event)-[:CHANGED_BY]-(asset)-[:CREATED_BY]-(specimen)
                    OPTIONAL MATCH (pipeline:Pipeline)<-[:USED]-(event)
                    OPTIONAL MATCH (workstation:Workstation)<-[:USED]-(event)
                    OPTIONAL MATCH (institution)-[:BELONGS_TO]-(asset)
                    RETURN event.timestamp, count(specimen), pipeline.name, workstation.name, institution.name
                $$) as (created_date agtype, specimens agtype, pipeline_name agtype, workstation_name agtype, institute_name agtype)
            """;

        return jdbi.withHandle(handle -> {
            handle.execute(DBConstants.AGE_BOILERPLATE);
            return handle.setSqlParser(new HashPrefixSqlParser())
                .createQuery(sql)
                .mapTo(GraphData.class)
                .list();
        });
    }
}
