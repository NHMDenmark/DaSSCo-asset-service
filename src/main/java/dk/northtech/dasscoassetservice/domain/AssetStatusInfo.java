package dk.northtech.dasscoassetservice.domain;

import java.time.Instant;

public record AssetStatusInfo(String asset_guid, String parent_guid, Instant error_timestamp, InternalStatus status, String error_message) {
}
