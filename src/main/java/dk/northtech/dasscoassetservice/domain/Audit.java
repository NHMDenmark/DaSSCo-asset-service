package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

// TODO: Does this need a description too?
public record Audit(@Schema(description = "The user doing the audit", example="THBO")String user) {
}
