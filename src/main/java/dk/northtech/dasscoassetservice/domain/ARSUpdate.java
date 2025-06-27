package dk.northtech.dasscoassetservice.domain;

import java.util.List;

public class ARSUpdate {
    public Asset asset;
    public boolean deleteAttachment = false;

    public ARSUpdate(Asset asset) {
        this.asset = asset;
    }

    public ARSUpdate(Asset asset, Boolean deleteAttachment) {
        this.asset = asset;
        this.deleteAttachment = deleteAttachment;
    }
}
