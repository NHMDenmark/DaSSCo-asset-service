package dk.northtech.dasscoassetservice.domain.specifyarssync;

import dk.northtech.dasscoassetservice.domain.MinimalAsset;

public class SyncParkingSpaceRequest {
    public MinimalAsset asset;
    public Long specifySyncLogId;

    public SyncParkingSpaceRequest(MinimalAsset asset, Long specifySyncLogId) {
        this.asset = asset;
        this.specifySyncLogId = specifySyncLogId;
    }
}
