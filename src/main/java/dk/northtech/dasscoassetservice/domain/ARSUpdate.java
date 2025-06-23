package dk.northtech.dasscoassetservice.domain;

import java.util.List;

public class ARSUpdate {
    public Asset asset;
    public List<DasscoFile> files;

    public ARSUpdate(Asset asset) {
        this.asset = asset;
    }

    public ARSUpdate(Asset asset, List<DasscoFile> files) {
        this.asset = asset;
        this.files = files;
    }
}
