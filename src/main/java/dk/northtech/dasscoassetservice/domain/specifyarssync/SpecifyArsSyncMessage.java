package dk.northtech.dasscoassetservice.domain.specifyarssync;



import dk.northtech.dasscoassetservice.domain.Asset;

import java.util.Set;

public class SpecifyArsSyncMessage {
    public Asset asset;
    //Those are the fields that have specifymappings
    public Set<String> updatedFields;
    public Long specifySyncLogId;
    public SpecifyArsSyncMessage() {
    }

    public SpecifyArsSyncMessage(Asset asset, Set<String> updatedFields, Long specifySyncLogId) {
        this.asset = asset;
        this.updatedFields = updatedFields;
        this.specifySyncLogId = specifySyncLogId;
    }
}
