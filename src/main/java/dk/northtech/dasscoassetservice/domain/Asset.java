package dk.northtech.dasscoassetservice.domain;

import dk.northtech.dasscoassetservice.webapi.domain.SambaInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Asset {
    @Schema(description = "See asset_guid. One possible PID is to construct a URL like pid.dassco.dk/GUID1234555677243. This is then the unique and resolvable identifier that we will use when sharing.", example = "asdf-1234-3333-1000")
    public String asset_pid;
    @Schema(description = "This is the unique GUID generated for each asset and is generated before incorporation into the storage system. Parts of the string are defined based on things such as the workstation and institution, the other parts are randomly generated. This is to enable a unique name for each asset. It is mandatory for our funding that we also have persistent identifiers for each asset (ideally resolvable as well). So we imagined an easy way to do this would be to incorporate the guid into a persistent identifier that can be clicked on to resolve (see asset_pid).", example = "ti-a01-202305241657")
    public String asset_guid;
    @Schema(description = "The status of the asset", example = "BEING_PROCESSED")
    public AssetStatus status;
    @Schema(description = "Basically a multispecimen is a single image (or other type of media) that actually contains multiple specimens in it", example = "false")
    public boolean multi_specimen;
    @Schema(description = "The barcodes of associated specimens", example = "'[\"ti-sp-00012\"']")
//    public List<String> specimen_barcodes = new ArrayList<>();
    public List<Specimen> specimens = new ArrayList<>();
    @Schema(description = "A short description of funding source used to create the asset", example = "Hundredetusindvis af dollars")
    public String funding;
    @Schema(description = "We will need to distinguish between image of a folder, device target, specimen, label etc)", example = "folder")
    public String subject;
    @Schema(description = "image, ct scan, surface scan, document", example = "ct scan")
    public String payload_type;
    @Schema(description = "File format enum, can contain multiple formats")
    public List<FileFormat> file_formats = new ArrayList<>();
    @Schema(description = "Flags if it is possible to edit / delete the media of this asset", example = "false")
    public boolean asset_locked;
    public List<Role> restricted_access = new ArrayList<>();

    @Schema(description = "A dictionary of dynamic properties")
    public Map<String, String> tags = new HashMap<>();
    @Schema(description = "Marking if this asset has been audited at least once", example = "true")
    public boolean audited;

    @Schema(description = "Date the asset metadata was uploaded", example = "2023-05-24T00:00:00.000Z")
    public Instant created_date;
    @Schema(description = "Date the asset metadata was last updated", example = "2023-05-24T00:00:00.000Z")
    public Instant date_metadata_updated;
    @Schema(description = "Date the asset was taken", example = "2023-05-24T00:00:00.000Z")
    public Instant date_asset_taken;
    @Schema(description = "Date the asset was marked as deleted in the metadata", example = "2023-05-24T00:00:00.000Z")
    public Instant date_asset_deleted;
    @Schema(description = "Date the asset was pushed to specify", example = "2023-05-24T00:00:00.000Z")
    public Instant date_asset_finalised;
    public Instant date_metadata_taken;

    //References
    @Schema(description = "The institution", example = "NNAD")
    public String institution;

    @Schema(description = "GUID of the parent asset", example = "ti-a02-202305241657")
    public String parent_guid;
    @Schema(description = "Name of the collection the asset belongs to", example = "test-collection")
    public String collection;
    @Schema(description = "The location on the storage where asset media can be uploaded")
    public SambaInfo sambaInfo;
    @Schema(description = "An internal status field used to track the status of the upload of related media", example = "COMPLETED")
    public InternalStatus internal_status;
    @Schema(description = "Username of the person that updated the asset", example = "THBO")
    public String updateUser;
    public List<Event> events;

    @Schema(description = "Username of the person that digitised the asset,", example = "THBO")
    public String digitiser;
    @Schema(description = "The name of the workstation that created or updated the asset", example = "ti-ws1")
    public String workstation;
    @Schema(description = "The pipeline that created or updated the asset", example = "ti-p1")
    public String pipeline;
}
