package dk.northtech.dasscoassetservice.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Schema(description = "Specimens are created together with Assets and inherit the institution and collection from the asset it was created with. If another asset is created with a specimen containing the same information it will be linked to the previously created specimen")
public final class Specimen {
    @Schema(description = "The name of the institution which owns and digitised the specimen", example = "test-institution")
    public  String institution;
    @Schema(description = "The collection name within the institution that holds the specimen", example = "test-collection")
    public  String collection;
    @Schema(description = "The barcodes of associated specimens", example = "'[\"ti-sp-00012\"']")
    public  String barcode;
    @Schema(description = "Persistent Identifier for the specimen")
    public  String specimen_pid;
    @Schema(description = "The way that the specimen has been prepared (pinned insect or mounted on a slide)", example = "slide")
    public  HashSet<String> preparation_types;
    public  Integer specimen_id;
    @JsonIgnore
    public  Integer collection_id;
    public  List<Role> role_restrictions = new ArrayList<>();

    public Specimen(
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
            Integer collection_id,
            @Schema(description = "The roles that are allowed to access assets connected to this specimen, empty if all users can read or write")
            List<Role> role_restrictions
    ) {
        this.institution = institution;
        this.collection = collection;
        this.barcode = barcode;
        this.specimen_pid = specimen_pid;
        this.preparation_types = preparation_types;
        this.specimen_id = specimen_id;
        this.collection_id = collection_id;
        this.role_restrictions = role_restrictions;
    }

    public Specimen(String barcode, String specimen_pid, HashSet<String> preparation_types) {
        this(null, null, barcode, specimen_pid, preparation_types, null, null, new ArrayList<>());
    }

    public Specimen(Specimen specimen, Integer specimen_id, Integer collecion_id) {
        this(specimen.institution, specimen.collection, specimen.barcode, specimen.specimen_pid, specimen.preparation_types, specimen_id, collecion_id, specimen.role_restrictions());
    }

    @JdbiConstructor
    public Specimen(Integer collection_id, Integer specimen_id, HashSet<String> preparation_types, String specimen_pid, String barcode, String collection, String institution) {
        this(institution, collection, barcode, specimen_pid, preparation_types, specimen_id, collection_id, new ArrayList<>());
    }

    public Specimen() {
    }

    @Schema(description = "The name of the institution which owns and digitised the specimen", example = "test-institution")
    public String institution() {
        return institution;
    }

    @Schema(description = "The collection name within the institution that holds the specimen", example = "test-collection")
    public String collection() {
        return collection;
    }

    @Schema(description = "The barcodes of associated specimens", example = "'[\"ti-sp-00012\"']")
    public String barcode() {
        return barcode;
    }

    @Schema(description = "Persistent Identifier for the specimen")
    public String specimen_pid() {
        return specimen_pid;
    }

    @Schema(description = "The way that the specimen has been prepared (pinned insect or mounted on a slide)", example = "slide")
    public HashSet<String> preparation_types() {
        return preparation_types;
    }

    public Integer specimen_id() {
        return specimen_id;
    }

    @JsonIgnore
    public Integer collection_id() {
        return collection_id;
    }

    public List<Role> role_restrictions() {
        return role_restrictions;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Specimen) obj;
        return Objects.equals(this.institution, that.institution) &&
               Objects.equals(this.collection, that.collection) &&
               Objects.equals(this.barcode, that.barcode) &&
               Objects.equals(this.specimen_pid, that.specimen_pid) &&
               Objects.equals(this.preparation_types, that.preparation_types) &&
               Objects.equals(this.specimen_id, that.specimen_id) &&
               Objects.equals(this.collection_id, that.collection_id) &&
               Objects.equals(this.role_restrictions, that.role_restrictions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(institution, collection, barcode, specimen_pid, preparation_types, specimen_id, collection_id, role_restrictions);
    }

    @Override
    public String toString() {
        return "Specimen[" +
               "institution=" + institution + ", " +
               "collection=" + collection + ", " +
               "barcode=" + barcode + ", " +
               "specimen_pid=" + specimen_pid + ", " +
               "preparation_types=" + preparation_types + ", " +
               "specimen_id=" + specimen_id + ", " +
               "collection_id=" + collection_id + ", " +
               "role_restrictions=" + role_restrictions + ']';
    }

}
