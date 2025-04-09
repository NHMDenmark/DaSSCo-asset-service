package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Digitiser;
import dk.northtech.dasscoassetservice.domain.Issue;
import jakarta.inject.Inject;
import org.apache.age.jdbc.base.Agtype;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.postgresql.jdbc.PgConnection;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;


public interface IssueRepository extends SqlObject {

    @SqlUpdate("""
            INSERT INTO issue(asset_guid, category,name,timestamp,status,description, notes, soleved)
            VALUES(:asset_guid, :category, :name, :timestamp, :status, :description, :notes, :solved);    
            """)
    void insert_issue();

    @SqlUpdate("""
            UPDATE issue SET category = :category
                , name = :name
                , status = :status
                , description = :description
                , notes = :notes
                , soleved = :solved
            WHERE issue_id = :issue_id
            """)
    void updateIssue(@BindMethods Issue issue);

    @SqlUpdate("DELETE FROM issue WHERE issue_id = :issue_id")
    void deleteIssue(Integer issue_id);

    @SqlQuery("""
        SELECT issue_id, asset_guid, category,name,timestamp,status,description, notes, soleved 
        FROM issue 
        WHERE asset_guid = :asset_guid; 
    """)
    List<Issue> findIssuesByAssetGuid();
}
