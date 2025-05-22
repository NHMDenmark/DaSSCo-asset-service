package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Legality;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface LegalityRepository extends SqlObject {
    @SqlUpdate("""
    INSERT INTO legality(copyright, license, credit)
    VALUES (:copyright, :license, :credit)
    RETURNING *
""")
    @GetGeneratedKeys
    Legality insertLegality(@BindMethods Legality legality);

    @SqlUpdate("""
            UPDATE legality
            SET copyright = :copyright
                , license = :license
                , credit = :credit
            WHERE legality_id = :legality_id
            """)
    void updateLegality(@BindMethods Legality legality);

    @SqlUpdate("""
           DELETE FROM legality
           WHERE legality_id = :legality_id
            """)
    void deleteLegality(long legality_id);
}
