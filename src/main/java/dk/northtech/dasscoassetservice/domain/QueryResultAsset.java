package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record QueryResultAsset(
        @Schema(description = "The Global Unique Identifier generated for each asset", example = "ti-a01-202305241657")
        String asset_guid,
        @Schema(description = "The name of the institution which owns and digitised the specimen", example = "test-institution")
        String institution,
        @Schema(description = "The collection name within the institution that holds the specimen", example = "test-collection")
        String collection,
        /*@Schema(description = "A list of specimen objects with the following information: institution, collection, preparation_type, barcode and specimen_pid")
        List<Specimen> specimens,*/
        @Schema(description = "The format of the asset", example = "[\"JPEG\"]")
        List<String> file_formats,
        @Schema(description = "Date and time the asset metadata was uploaded", example = "2023-05-24T00:00:00.000Z")
        Instant created_date
        /*@Schema(description = "List of the events associated with an asset")
        List<Event> events*/
) {
}
