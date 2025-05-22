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
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.postgresql.jdbc.PgConnection;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;


public interface WorkstationRepository extends SqlObject {


    @SqlUpdate(""" 
            INSERT INTO workstation(workstation_name, workstation_status, institution_name)
            VALUES (:name, :status, :institution_name)
            RETURNING workstation_name AS name
            , workstation_status AS status
            , institution_name
            , workstation_id 
            """
    )
    @GetGeneratedKeys
    public Workstation persistWorkstation(@BindMethods Workstation workstation);

    @SqlUpdate("UPDATE workstation SET workstation_status = :status WHERE workstation_id = :workstation_id")
    public void updateWorkstation(@BindMethods Workstation workstation);

    @SqlQuery("""
            SELECT workstation_name AS name
                   , workstation_status AS status
                   , institution_name
                   , workstation_id
            FROM workstation;
               """)
    public List<Workstation> listWorkStations();

}
