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
    @Schema(description = "Persistent Identifier for the asset", example = "asdf-1234-3333-1000")
    public String asset_pid;
    @Schema(description = "The Global Unique Identifier generated for each asset", example = "ti-a01-202305241657")
    public String asset_guid;
    @Schema(description = "The current status of an asset", example = "BEING_PROCESSED")
    public AssetStatus status;
    @Schema(description = "A single image (or other type of media) that contains multiple specimens in it. One asset is linked to multiple specimens", example = "false")
    public boolean multi_specimen;
    //@Schema(description = "The barcodes of associated specimens", example = "'[\"ti-sp-00012\"']")
    //public List<String> specimen_barcodes = new ArrayList<>();
    @Schema(description = "A list of specimen objects with the following information: institution, collection, preparation_type, barcode and specimen_pid")
    public List<Specimen> specimens = new ArrayList<>();
    @Schema(description = "A short description of funding source used to create the asset", example = "Hundredetusindvis af dollars")
    public String funding;
    // TODO: Subject is not present in Confluence. What does it refer to?
    @Schema(description = "We will need to distinguish between image of a folder, device target, specimen, label etc)", example = "folder")
    public String subject;
    @Schema(description = "What the asset represents (image, ct scan, surface scan, document)", example = "ct scan")
    public String payload_type;
    @Schema(description = "The format of the asset", example = "JPEG")
    public List<FileFormat> file_formats = new ArrayList<>();
    @Schema(description = "Flags if it is possible to edit / delete the media of this asset", example = "false")
    public boolean asset_locked;
    // TODO: Does this mean the User can access or can not access?
    @Schema(description = "List of possible roles for users", example = "ADMIN")
    public List<Role> restricted_access = new ArrayList<>();

    @Schema(description = "A dictionary of dynamic properties")
    public Map<String, String> tags = new HashMap<>();
    @Schema(description = "Records if the asset has been manually audited", example = "true")
    public boolean audited;

    @Schema(description = "Date and time the asset metadata was uploaded", example = "2023-05-24T00:00:00.000Z")
    public Instant created_date;
    @Schema(description = "Date and time the asset metadata was last updated", example = "2023-05-24T00:00:00.000Z")
    public Instant date_metadata_updated;
    @Schema(description = "Date and time when the original raw image was taken", example = "2023-05-24T00:00:00.000Z")
    public Instant date_asset_taken;
    @Schema(description = "Date and time the asset was marked as deleted in the metadata", example = "2023-05-24T00:00:00.000Z")
    public Instant date_asset_deleted;
    @Schema(description = "Date and time the asset was pushed to Specify", example = "2023-05-24T00:00:00.000Z")
    public Instant date_asset_finalised;
    @Schema(description = "Date and time of when the original raw image was taken", example = "2023-05-24T00:00:00.000Z")
    public Instant date_metadata_taken;

    //References
    @Schema(description = "The name of the institution which owns and digitised the specimen", example = "test-institution")
    public String institution;

    @Schema(description = "Name of the parent media (in most cases, the same as original_parent_name, it can be different if it is a derivative of a derivative)", example = "ti-a02-202305241657")
    public String parent_guid;
    @Schema(description = "The collection name within the institution that holds the specimen", example = "test-collection")
    public String collection;
    @Schema(description = "The location on the storage where asset media can be uploaded")
    public HttpInfo httpInfo;
    @Schema(description = "An internal status field used to track the status of the upload of related media", example = "COMPLETED")
    public InternalStatus internal_status;
    @Schema(description = "Username of the person that updated the asset", example = "THBO")
    public String updateUser;
    // TODO: Does not appear in Confluence. If we want to add a description I need to know what it is.
    @Schema(description = "")
    public List<Event> events;

    @Schema(description = "The name of the person who imaged the specimens (creating the assets)", example = "THBO")
    public String digitiser;
    @Schema(description = "The name of the workstation used to do the imaging", example = "ti-ws1")
    public String workstation;
    @Schema(description = "The name of the pipeline that sent a create, update or delete request to the storage service", example = "ti-p1")
    public String pipeline;
    @Schema(description = "If an error happened during digitisation of the asset an error message can be displayed here", example = "Failed to upload to ERDA: connection reset")
    public String error_message;
    @Schema(description = "Date and time that the error happened", example = "2023-05-24T00:00:00.000Z")
    public Instant error_timestamp;
}
