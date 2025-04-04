package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Funding;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;


public interface FundingRepository extends SqlObject {
    @SqlUpdate("INSERT INTO funding(funding) VALUES (:funding)")
    @GetGeneratedKeys
    public Funding insertFunding(String funding);

    @SqlQuery("SELECT * FROM funding")
    List<Funding> listFunds();

    @SqlUpdate("INSERT INTO asset_funding(asset_guid, funding_id) VALUES (:asset_guid, :funding_id)")
    void fundAsset(String asset_guid, Integer funding_id);

    @SqlUpdate("DELETE FROM asset_funding WHERE asset_guid = :assetGuid AND funding_id = :funding_id")
    void deFundAsset(String assetGuid, Integer funding_id);

    @SqlQuery("""
            SELECT funding_id
                , funding
            FROM asset_funding
                INNER JOIN funding USING (funding_id)
            WHERE asset_guid = :asset_guid    
            """)
    List<Funding> getAssetFunds(String asset_guid);
}
