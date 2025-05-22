package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Publication;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface PublisherRepository extends SqlObject {


    @SqlUpdate("""
        INSERT INTO asset_publisher(description, publisher, asset_guid) 
        VALUES (:description, :name, :asset_guid)
        RETURNING asset_guid
        , description
        , publisher AS name
        , asset_publisher_id AS publication_id
    """)
    @GetGeneratedKeys
    Publication internal_publish(@BindMethods Publication publication);

    @SqlQuery("""
    SELECT asset_guid
        , description
        , publisher AS name
        , asset_publisher_id AS publication_id
       FROM asset_publisher
    WHERE asset_guid = :assetGuid
    """)
    List<Publication> internal_listPublicationLinks(String assetGuid);

    @SqlUpdate("""
            UPDATE asset_publisher 
                SET description = :description 
                , publisher = :name
                WHERE asset_publisher_id = :publication_id
            """ )
    void update(@BindMethods Publication publication);

    @SqlUpdate("DELETE FROM asset_publisher WHERE asset_publisher_id = :asset_publisher_id")
    void delete(long asset_publisher_id);

}
