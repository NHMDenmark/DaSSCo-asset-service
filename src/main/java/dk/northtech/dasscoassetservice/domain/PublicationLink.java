package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "URL to external publisher sites to which the asset is published")
public record PublicationLink (
        @Schema(description = "The Global Unique Identifier generated for each asset", example = "ti-a01-202305241657")
        String asset_guid,
        @Schema(description = "The URL for the publisher site")
        String link,
        @Schema(description = "Name of the publisher")
        String publisher_name,
        @Schema(description = "Date and time of the publication", example = "2023-05-24T00:00:00.000Z")
        Instant timestamp) {

}
