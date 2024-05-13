package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.AssetStatus;
import dk.northtech.dasscoassetservice.domain.AssetStatusInfo;
import dk.northtech.dasscoassetservice.domain.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;

public class InternalStatusServiceTest extends AbstractIntegrationTest {

    @Test
    void testGetAssetStatus(){
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.asset_guid = "testGetAssetStatus";
        asset.asset_pid = "pid-testGetAssetStatus";
        asset.pipeline = "i2_p1";
        asset.workstation = "i2_w1";
        asset.collection = "i2_c1";
        asset.status = AssetStatus.BEING_PROCESSED;
        assetService.persistAsset(asset, new User("test-user"), 1);
        Optional<AssetStatusInfo> optAsset = internalStatusService.getAssetStatus(asset.asset_guid);
        assertThat(optAsset.isPresent()).isTrue();
        AssetStatusInfo assetInfo = optAsset.get();
        assertThat(assetInfo.asset_guid()).isEqualTo("testGetAssetStatus");
        assertThat(assetInfo.status().toString()).isEqualTo("METADATA_RECEIVED");
    }
}
