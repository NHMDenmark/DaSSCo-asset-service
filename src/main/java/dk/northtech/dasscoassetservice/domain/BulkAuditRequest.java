package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Request object for bulk auditing multiple assets")
public record BulkAuditRequest(
        @Schema(description = "The user performing the audit", example = "THBO")
        String user,
        @Schema(description = "List of asset GUIDs to audit", example = "[\"asset-001\", \"asset-002\"]")
        List<String> assetGuids
) {}
