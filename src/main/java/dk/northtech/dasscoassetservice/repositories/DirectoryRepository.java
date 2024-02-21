package dk.northtech.dasscoassetservice.repositories;


import dk.northtech.dasscoassetservice.domain.Directory;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;

public interface DirectoryRepository {

    //Find writeable directories.
    @SqlQuery("""
        SELECT d.*, sa.asset_guid FROM dassco_file_proxy.directories d
            LEFT JOIN dassco_file_proxy.shared_assets sa ON sa.directory_id = d.directory_id 
        WHERE d.access = 'WRITE'::dassco_file_proxy.access_type 
""")
    List<Directory> getWriteableDirectories();

}
