package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.Workstation;
import dk.northtech.dasscoassetservice.domain.WorkstationStatus;
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
public class WorkstationRepository {

    private Jdbi jdbi;
    private DataSource dataSource;

    @Inject
    public WorkstationRepository(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public void persistWorkstation(Workstation workstation) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MERGE (i:Institution {name: $institution_name})
                            MERGE (c:Workstation {name: $workstation_name, status: $workstation_status})
                            MERGE (i)<-[:STATIONED_AT]-(c)
                            RETURN i.name, c.name
                        $$
                        , #params) as (institution_name agtype, workstation_name agtype);
                        """;


        try {
            jdbi.withHandle(handle -> {
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                AgtypeMap name = new AgtypeMapBuilder()
                        .add("institution_name", workstation.institution_name())
                        .add("workstation_name", workstation.name())
                        .add("workstation_status", workstation.status().name()).build();
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

    public void updateWorkstation(Workstation workstation) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                             MATCH (w:Workstation{name: $workstation_name})
                             SET w.status = $workstation_status
                        $$
                        , #params) as (w agtype);
                        """;


        try {
            jdbi.withHandle(handle -> {
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                AgtypeMap name = new AgtypeMapBuilder()
//                        .add("institution_name", workstation.institution_name())
                        .add("workstation_name", workstation.name())
                        .add("workstation_status", workstation.status().name()).build();
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

    public Optional<Workstation> findWorkstation(String workstationName) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                         , $$
                             MATCH (w:Workstation)-[:STATIONED_AT]->(i:Institution)
                             WHERE w.name = $workstation_name
                             RETURN w.name, w.status, i.name
                           $$
                         , #params
                        ) as (workstation_name agtype, status agtype, institution_name agtype);""";


        try {
            return jdbi.withHandle(handle -> {
                // We have to register the type
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                AgtypeMap name = new AgtypeMapBuilder().add("workstation_name", workstationName).build();
                Agtype agtype = AgtypeFactory.create(name);
                handle.execute(DBConstants.AGE_BOILERPLATE);
                return handle.createQuery(sql).bind("params", agtype)
                        .map((rs, ctx) -> {
                            Agtype workstation_name = rs.getObject("workstation_name", Agtype.class);
                            Agtype workstation_status = rs.getObject("status", Agtype.class);
                            Agtype institutionName = rs.getObject("institution_name", Agtype.class);
                            return new Workstation(workstation_name.getString(), WorkstationStatus.valueOf(workstation_status.getString()),institutionName.getString());
                        }).findFirst();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Workstation> listWorkStations(Institution institution) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                         , $$
                             MATCH (w:Workstation)-[:STATIONED_AT]->(i:Institution)
                             WHERE i.name = $institution_name
                             RETURN w.name, w.status 
                           $$
                         , #params
                        ) as (workstation_name agtype, status agtype);""";


        try {
            return jdbi.withHandle(handle -> {
                // We have to register the type
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);

                AgtypeMap name = new AgtypeMapBuilder()
                        .add("institution_name", institution.name()).build();
                Agtype agtype = AgtypeFactory.create(name);
                handle.execute(DBConstants.AGE_BOILERPLATE);
                return handle.createQuery(sql).bind("params", agtype)
                        .map((rs, ctx) -> {
                            Agtype workstation_name = rs.getObject("workstation_name", Agtype.class);
                            Agtype status = rs.getObject("status", Agtype.class);
                            return new Workstation(workstation_name.getString(),WorkstationStatus.valueOf(status.getString()),institution.name());
                        }).list();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
