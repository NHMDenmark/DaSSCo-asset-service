package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.DigitiserLink;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

@RegisterConstructorMapper(DigitiserLink.class)
public interface DigitiserListRepository extends SqlObject {
    @SqlQuery("""
        SELECT dl.digitiser_list_id,
               dl.dassco_user_id,
               du.username,
               dl.asset_guid
          FROM digitiser_list dl
          JOIN dassco_user du ON dl.dassco_user_id = du.dassco_user_id
         WHERE dl.asset_guid IN (<assetGuids>)
        """)
    List<DigitiserLink> listDigitisersByAssetGuids(@BindList List<String> assetGuids);


    @SqlUpdate("""
        INSERT INTO digitiser_list (dassco_user_id, asset_guid)
        VALUES (:dasscoUserId, :assetGuid)
        """)
    void insertLink(@Bind("dasscoUserId") Integer dasscoUserId,
                    @Bind("assetGuid") String assetGuid);
}