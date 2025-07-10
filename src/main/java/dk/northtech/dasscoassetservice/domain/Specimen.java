package dk.northtech.dasscoassetservice.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

import java.util.HashSet;
import java.util.Set;

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
        HashSet<String> preparation_types,
        @Schema(description = "A Specimen can have multiple different preparation_types, this field is used to specify what preparation_type is valid for the asset this specimen is connected to", example = "slide")
        String asset_preparation_type,
//        @JsonIgnore
        Integer specimen_id,
        @JsonIgnore
        Integer collection_id,
        @Schema(description = "The id that connects an attachment to a Collection Object in specify, can be seen as an analogue to the asset_specimen junction in ARS", example = "1234")
        Long specify_collection_object_attachment_id,
        @Schema(description = "If a Specimen has been detached from an Asset in ARS after it have been synchronized to Specify this is set to true until specify has been synchronised again and the asset_specimen connection can be safely deleted")
        boolean asset_detached
    ) {
    public Specimen(String barcode, String specimen_pid, HashSet<String> preparation_types, String asset_preparation_type) {
        this(null, null, barcode, specimen_pid, preparation_types, asset_preparation_type, null, null, null, false);
    }

    @JdbiConstructor
    public Specimen {
    }
//    new Specimen(createAsset.institution, "i1_c1", "creatAsset-sp-2", "spid2", new HashSet<>(Set.of("pinning")),"pinning",
    public Specimen(String institution, String collection, String barcode, String specimen_pid, HashSet<String> preparation_types, String asset_preparation_type) {
        this(institution, collection, barcode, specimen_pid, preparation_types, asset_preparation_type,null, null, null, false);
    }
}
