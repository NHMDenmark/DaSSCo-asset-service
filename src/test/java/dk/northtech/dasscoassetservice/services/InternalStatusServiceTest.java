package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.AssetStatusInfo;
import dk.northtech.dasscoassetservice.domain.InternalStatus;
import dk.northtech.dasscoassetservice.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static dk.northtech.dasscoassetservice.services.AssetServiceTest.getTestAsset;
import static org.junit.jupiter.api.Assertions.*;
class InternalStatusServiceTest extends AbstractIntegrationTest{
    @BeforeEach
    void init() {
        if (user == null) {
            user = userService.ensureExists(new User("Teztuzer"));
        }
    }

    User user = null;
    @Test
    void testAssetStatus() {
        Asset asset = getTestAsset("testAssetStatus");
        assetService.persistAsset(asset, user, 777);
        assetService.setAssetStatus("testAssetStatus", InternalStatus.ERDA_ERROR.name(), "Epic fail");
        Optional<AssetStatusInfo> testAssetStatus = internalStatusService.getAssetStatus("testAssetStatus");
        assertThat(testAssetStatus.isPresent()).isTrue();
        AssetStatusInfo assetStatusInfo = testAssetStatus.get();
        assertThat(assetStatusInfo.error_message()).isEqualTo("Epic fail");
        assertThat(assetStatusInfo.status()).isEqualTo(InternalStatus.ERDA_ERROR);
    }

    @Test
    void testGetFailedAssets() {
        Asset asset = getTestAsset("testGetFailedAssets_failed");
        Asset not_failed = getTestAsset("testGetFailedAssets_not_failed");
        assetService.persistAsset(asset, user, 777);
        assetService.persistAsset(not_failed, user, 777);
        assetService.setAssetStatus("testGetFailedAssets_failed", InternalStatus.ERDA_ERROR.name(), "Oh no");
        List<AssetStatusInfo> workInProgressAssets = internalStatusService.getWorkInProgressAssets(true);
        Optional<AssetStatusInfo> testGetFailedAssets = workInProgressAssets.stream()
                .filter(x -> x.asset_guid().equals("testGetFailedAssets_failed"))
                .findFirst();
        assertThat(testGetFailedAssets.isPresent()).isTrue();
        AssetStatusInfo assetStatusInfo = testGetFailedAssets.get();
        assertThat(assetStatusInfo.error_message()).isEqualTo("Oh no");
        //Only failed should be present
        workInProgressAssets.stream()
                .filter(x -> x.asset_guid().equals("testGetFailedAssets_not_failed"))
                .findFirst()
                .ifPresent(x -> fail("Only failed should be found"));

        List<AssetStatusInfo> allWorkInProgressAssets = internalStatusService.getWorkInProgressAssets(false);
        Optional<AssetStatusInfo> testGetFailedAssetsNotFailed = allWorkInProgressAssets.stream()
                .filter(x -> x.asset_guid().equals("testGetFailedAssets_not_failed"))
                .findFirst();
        assertWithMessage("All in progress assets should be found, onlyFailed is set to false")
                .that(testGetFailedAssetsNotFailed.isPresent()).isTrue();

    }

}