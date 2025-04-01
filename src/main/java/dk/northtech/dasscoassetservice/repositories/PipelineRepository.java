package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.Pipeline;
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


public interface PipelineRepository extends SqlObject {



    @SqlUpdate("""
    INSERT INTO pipeline(pipeline_name, institution_name) 
    VALUES (:name, :institution)
    RETURNING pipeline_name AS name
        , institution_name AS institution
        , pipeline_id
    """)
    @GetGeneratedKeys("pipeline_id")
    public Pipeline persistPipeline(@BindMethods Pipeline pipeline);

    @SqlQuery("SELECT pipeline_name AS name, institution_name AS institution, pipeline_id FROM pipeline")
    public List<Pipeline> listPipelines();
}
