package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Pipeline;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;


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
