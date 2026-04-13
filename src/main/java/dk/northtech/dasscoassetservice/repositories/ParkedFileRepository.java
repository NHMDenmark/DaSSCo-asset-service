package dk.northtech.dasscoassetservice.repositories;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface ParkedFileRepository {

    @SqlQuery("""
        SELECT EXISTS (
            SELECT 1
            FROM parked_file
            WHERE ltrim(path, '/') LIKE :pathPrefix || '%'
        )
        """)
    boolean existsByPathPrefix(@Bind("pathPrefix") String pathPrefix);
}
