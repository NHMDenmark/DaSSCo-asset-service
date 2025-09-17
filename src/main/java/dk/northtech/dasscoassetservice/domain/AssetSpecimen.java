package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

import javax.annotation.Nullable;

public class AssetSpecimen {
    public Integer specimen_id;
    public String asset_guid;
    public String specimen_pid;
    public Long asset_specimen_id;
    @Schema(description = "A Specimen can have multiple different preparation_types, this field is used to specify what preparation_type is valid for the asset this specimen is connected to", example = "slide")
    public String asset_preparation_type;
    @Schema(description = "The id that connects an attachment to a Collection Object in specify, can be seen as an analogue to the asset_specimen junction in ARS", example = "1234")
    public Long specify_collection_object_attachment_id;
    @Schema(description = "If a Specimen has been detached from an Asset in ARS after it have been synchronized to Specify this is set to true until specify has been synchronised again and the asset_specimen connection can be safely deleted")
    public boolean asset_detached;
    @Nullable
    public Specimen specimen;

    public AssetSpecimen(String asset_guid, String specimen_pid, String asset_preparation_type, boolean asset_detached) {
        this.asset_guid = asset_guid;
        this.specimen_pid = specimen_pid;
        this.asset_preparation_type = asset_preparation_type;
        this.asset_detached = asset_detached;
    }

    @JdbiConstructor()
    public AssetSpecimen(boolean asset_detached, Long specify_collection_object_attachment_id, String asset_preparation_type, Long asset_specimen_id, String specimen_pid, String asset_guid, Integer specimen_id) {
        this.asset_detached = asset_detached;
        this.specify_collection_object_attachment_id = specify_collection_object_attachment_id;
        this.asset_preparation_type = asset_preparation_type;
        this.asset_specimen_id = asset_specimen_id;
        this.specimen_pid = specimen_pid;
        this.asset_guid = asset_guid;
        this.specimen_id = specimen_id;
    }
}
