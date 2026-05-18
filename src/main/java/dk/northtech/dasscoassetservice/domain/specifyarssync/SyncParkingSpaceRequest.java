package dk.northtech.dasscoassetservice.domain.specifyarssync;

import dk.northtech.dasscoassetservice.domain.MinimalAsset;

public class SyncParkingSpaceRequest {
    public MinimalAsset asset;
    public Long specifySyncLogId;
    public String attachmentLocation;

    public SyncParkingSpaceRequest(MinimalAsset asset, Long specifySyncLogId, String attachmentLocation) {
        this.asset = asset;
        this.specifySyncLogId = specifySyncLogId;
        this.attachmentLocation = attachmentLocation;
    }
}
