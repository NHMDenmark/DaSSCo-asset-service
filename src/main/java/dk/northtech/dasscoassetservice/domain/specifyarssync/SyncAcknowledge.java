package dk.northtech.dasscoassetservice.domain.specifyarssync;

public record SyncAcknowledge(SpecifySyncStatus specifySyncStatus, Long specifySyncLogId, String additionalInfo, String assetGuid)  {

}
