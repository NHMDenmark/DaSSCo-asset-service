package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Digitiser;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;

@RegisterConstructorMapper(Digitiser.class)
public interface DigitiserRepository extends SqlObject {

    @SqlQuery("""
        SELECT
            dassco_user_id AS "dasscoUserId",
            username
        FROM dassco_user
        ORDER BY dassco_user_id
        """)
    List<Digitiser> listDigitisers();

    @SqlQuery("""
            SELECT dassco_user_id AS dasscoUserId
                 , username
              FROM dassco_user
             WHERE dassco_user_id = :dasscoUserId
            """)
    Digitiser findDigitiserById(String dasscoUserId);

}