package dk.northtech.dasscoassetservice.domain;

import dk.northtech.dasscoassetservice.webapi.domain.HttpInfo;
import dk.northtech.dasscoassetservice.webapi.domain.SambaInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Asset {
    // TODO: Change this to make it clearer.
    @Schema(description = "See asset_guid. One possible PID is to construct a URL like pid.dassco.dk/GUID1234555677243. This is then the unique and resolvable identifier that we will use when sharing.", example = "asdf-1234-3333-1000")
    public String asset_pid;
    // TODO: Change this to make it clearer.
    @Schema(description = "This is the unique GUID generated for each asset and is generated before incorporation into the storage system. Parts of the string are defined based on things such as the workstation and institution, the other parts are randomly generated. This is to enable a unique name for each asset. It is mandatory for our funding that we also have persistent identifiers for each asset (ideally resolvable as well). So we imagined an easy way to do this would be to incorporate the guid into a persistent identifier that can be clicked on to resolve (see asset_pid).", example = "ti-a01-202305241657")
    public String asset_guid;
    @Schema(description = "The current status of an asset", example = "BEING_PROCESSED")
    public AssetStatus status;
    @Schema(description = "Basically a multispecimen is a single image (or other type of media) that actually contains multiple specimens in it. One asset is linked to multiple specimens", example = "false")
    public boolean multi_specimen;
    //@Schema(description = "The barcodes of associated specimens", example = "'[\"ti-sp-00012\"']")
    //public List<String> specimen_barcodes = new ArrayList<>();
    // TODO: Do I need to add Institution and Collection? They are null in the Specimen constructor.
    @Schema(description = "A list of specimen objects with the following information: preparation_type, barcode and specimen_pid")
    public List<Specimen> specimens = new ArrayList<>();
    @Schema(description = "A short description of funding source used to create the asset", example = "Hundredetusindvis af dollars")
    public String funding;
    // TODO: Subject is not present in Confluence.
    @Schema(description = "We will need to distinguish between image of a folder, device target, specimen, label etc)", example = "folder")
    public String subject;
    @Schema(description = "What the asset represents (image, ct scan, surface scan, document)", example = "ct scan")
    public String payload_type;
    @Schema(description = "The format of the asset", example = "JPEG")
    public List<FileFormat> file_formats = new ArrayList<>();
    @Schema(description = "Flags if it is possible to edit / delete the media of this asset", example = "false")
    public boolean asset_locked;
    // TODO: Does this mean the User can see or can not see the file?
    @Schema(description = "", example = "ADMIN")
    public List<Role> restricted_access = new ArrayList<>();

    // TODO: Check what to write here. Confluence says different.
    @Schema(description = "A dictionary of dynamic properties")
    public Map<String, String> tags = new HashMap<>();
    // TODO: Appears as V2.
    @Schema(description = "Records if the asset has been manually audited", example = "true")
    public boolean audited;

    // TODO: Check. In Confluence appears as "date_asset_created".
    @Schema(description = "Date and time the asset metadata was uploaded", example = "2023-05-24T00:00:00.000Z")
    public Instant created_date;
    // TODO: Different in Confluence: "Set when the metadata is updated"
    @Schema(description = "Date and time the asset metadata was last updated", example = "2023-05-24T00:00:00.000Z")
    public Instant date_metadata_updated;
    @Schema(description = "Date and time when the original raw image was taken", example = "2023-05-24T00:00:00.000Z")
    public Instant date_asset_taken;
    // TODO: Does not have a description in Confluence.
    @Schema(description = "Date and time the asset was marked as deleted in the metadata", example = "2023-05-24T00:00:00.000Z")
    public Instant date_asset_deleted;
    // TODO: Does not have a description in Confluence.
    @Schema(description = "Date and time the asset was pushed to specify", example = "2023-05-24T00:00:00.000Z")
    public Instant date_asset_finalised;
    @Schema(description = "Date and time of when the original raw image was taken", example = "2023-05-24T00:00:00.000Z")
    public Instant date_metadata_taken;

    //References
    @Schema(description = "The name of the institution which owns and digitised the specimen", example = "NNAD")
    public String institution;

    @Schema(description = "Name of the parent media (in most cases, the same as original_parent_name, it can be different if it is a derivative of a derivative)", example = "ti-a02-202305241657")
    public String parent_guid;
    @Schema(description = "The collection name within the institution that holds the specimen", example = "test-collection")
    public String collection;
    // TODO: Check. Is not available in Confluence.
    @Schema(description = "The location on the storage where asset media can be uploaded")
    public HttpInfo httpInfo;
    // TODO: Check. Description is different in Confluence.
    @Schema(description = "An internal status field used to track the status of the upload of related media", example = "COMPLETED")
    public InternalStatus internal_status;
    // TODO: Appears as "update_user" in Confluence.
    @Schema(description = "Username of the person that updated the asset", example = "THBO")
    public String updateUser;
    // TODO: Not available in Confluence.
    public List<Event> events;

    @Schema(description = "The name of the person who imaged the specimens (creating the assets)", example = "THBO")
    public String digitiser;
    // TODO: Appears as "workstation_name" in Confluence.
    @Schema(description = "The name of the workstation used to do the imaging", example = "ti-ws1")
    public String workstation;
    // TODO: Appears as "pipeline_name" in Confluence.
    @Schema(description = "The name of the pipeline that sent a create, update or delete request to the storage service", example = "ti-p1")
    public String pipeline;
    @Schema(description = "If an error happened during digitisation of the asset an error message can be displayed here", example = "Failed to upload to ERDA: connection reset")
    public String error_message;
    @Schema(description = "Date and time that the error happened", example = "2023-05-24T00:00:00.000Z")
    public Instant error_timestamp;
}
