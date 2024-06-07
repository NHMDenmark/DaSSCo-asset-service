package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Specimens are created together with Assets and inherit the institution and collection from the asset it was created with. If another asset is created with a specimen containing the same information it will be linked to the previously created specimen")
public record Specimen(
        @Schema(description = "The name of the institution which owns and digitised the specimen", example = "test-institution")
        String institution,
        @Schema(description = "The collection name within the institution that holds the specimen", example = "test-collection")
        String collection,
        @Schema(description = "The barcodes of associated specimens", example = "'[\"ti-sp-00012\"']")
        String barcode,
        @Schema(description = "Persistent Identifier for the specimen")
        String specimen_pid,
        @Schema(description = "The way that the specimen has been prepared (pinned insect or mounted on a slide)", example = "slide")
        String preparation_type) {
    public Specimen(String barcode, String specimen_pid, String preparation_type) {
        this(null, null, barcode, specimen_pid, preparation_type);
    }
}
