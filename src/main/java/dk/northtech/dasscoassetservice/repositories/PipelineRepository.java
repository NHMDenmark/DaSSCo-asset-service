package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Pipeline;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
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
import java.util.List;
import java.util.Optional;

@Repository
public class PipelineRepository {
    private Jdbi jdbi;
    private DataSource dataSource;

    @Inject
    public PipelineRepository(Jdbi jdbi, DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbi = jdbi;
    }


    public void persistPipeline(Pipeline pipeline) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (i:Institution {name: $institution_name})
                            MERGE (p:Pipeline {name: $pipeline_name})
                            MERGE (i)<-[:USED_BY]-(p)
                            RETURN i.name, p.name
                        $$
                        , #params) as (institution_name agtype, pipeline_name agtype);
                        """;


        try {
            jdbi.withHandle(handle -> {
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                AgtypeMap name = new AgtypeMapBuilder()
                        .add("institution_name", pipeline.institution())
                        .add("pipeline_name", pipeline.name()).build();
                Agtype agtype = AgtypeFactory.create(name);
                handle.execute(DBConstants.AGE_BOILERPLATE);
                handle.createUpdate(sql)
                        .bind("params", agtype)
                        .execute();
                handle.close();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Pipeline> listPipelines(Institution institution) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                         , $$
                             MATCH (p:Pipeline)-[:USED_BY]->(i:Institution)
                             WHERE i.name = $institution_name
                             RETURN p.name
                           $$
                         , #params
                        ) as (name agtype);""";


        try {
            return jdbi.withHandle(handle -> {
                // We have to register the type
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);

                AgtypeMap name = new AgtypeMapBuilder().add("institution_name", institution.name()).build();
                Agtype agtype = AgtypeFactory.create(name);
                handle.execute(DBConstants.AGE_BOILERPLATE);
                return handle.createQuery(sql).bind("params", agtype)
                        .map((rs, ctx) -> {
                            Agtype pipeline_name = rs.getObject("name", Agtype.class);
                            return new Pipeline(pipeline_name.getString(), institution.name());
                        }).list();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Pipeline> findPipeline(String pipelineName) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                         , $$
                             MATCH (p:Pipeline{name: $pipeline_name})-[:USED_BY]->(i:Institution)
                             RETURN p.name, i.name
                           $$
                         , #params
                        ) as (pipeline_name agtype, institution_name agtype);""";


        try {
            return jdbi.withHandle(handle -> {
                // We have to register the type
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                AgtypeMap name = new AgtypeMapBuilder().add("pipeline_name", pipelineName).build();
                Agtype agtype = AgtypeFactory.create(name);
                handle.execute(DBConstants.AGE_BOILERPLATE);
                return handle.createQuery(sql).bind("params", agtype)
                        .map((rs, ctx) -> {
                            Agtype pipeline_name = rs.getObject("pipeline_name", Agtype.class);
                            Agtype institution_name = rs.getObject("institution_name", Agtype.class);
                            return new Pipeline(pipeline_name.getString(), institution_name.getString());
                        }).findOne();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
