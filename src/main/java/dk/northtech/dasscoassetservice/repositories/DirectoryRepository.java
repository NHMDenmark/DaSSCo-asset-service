package dk.northtech.dasscoassetservice.repositories;


import dk.northtech.dasscoassetservice.domain.Directory;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface DirectoryRepository {

    @SqlQuery("""
        SELECT d.*, sa.asset_guid 
        FROM directories d 
            LEFT JOIN shared_assets sa ON sa.directory_id = d.directory_id 
        WHERE d.access = 'WRITE'::access_type 
""")
    List<Directory> getWriteableDirectories();
    //Find a directory by assetGuid. Should only return one as only single asset directories will have write access.
    @SqlQuery("""
        SELECT d.*, sa.asset_guid 
        FROM directories d 
            LEFT JOIN shared_assets sa ON sa.directory_id = d.directory_id 
        WHERE d.access = 'WRITE'::access_type AND sa.asset_guid = :asset_guid
""")
    Directory getWriteableDirectory(String asset_guid);
}
