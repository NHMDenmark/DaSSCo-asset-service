package dk.northtech.dasscoassetservice.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
//        @JsonIgnore
        Integer specimen_id,
        @JsonIgnore
        Integer collection_id,

        List<Role> role_restrictions
    ) {
    public Specimen(String barcode, String specimen_pid, HashSet<String> preparation_types) {
        this(null, null, barcode, specimen_pid, preparation_types, null, null, new ArrayList<>());
    }

    public Specimen(Specimen specimen, Integer specimen_id ,Integer collecion_id) {
        this(specimen.institution, specimen.collection, specimen.barcode, specimen.specimen_pid, specimen.preparation_types, specimen_id,collecion_id, specimen.role_restrictions());
    }

    @JdbiConstructor
    public Specimen(Integer collection_id, Integer specimen_id, HashSet<String> preparation_types, String specimen_pid, String barcode, String collection, String institution) {
        this(institution, collection, barcode, specimen_pid, preparation_types, specimen_id, collection_id, List.of());
    }
}
