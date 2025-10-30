package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.AssetChange;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Set;


public interface AssetChangeRepository extends SqlObject {

    @SqlUpdate("insert into asset_change (change, dassco_user_id, directory_id, asset_guid) values (:change, :dassco_user_id, :directory_id, :asset_guid)")
    int create(@BindMethods AssetChange assetChange);

    @SqlQuery("select * from asset_change where directory_id = :directory_id")
    Set<AssetChange> getAll(Long directory_id);

    @SqlUpdate("delete from asset_change where directory_id = :directory_id")
    void deleteAll(Long directory_id);
}
