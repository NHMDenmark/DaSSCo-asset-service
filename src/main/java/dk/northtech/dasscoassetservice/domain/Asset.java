package dk.northtech.dasscoassetservice.domain;

import dk.northtech.dasscoassetservice.webapi.domain.HttpInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.*;

public class Asset {
    @Schema(description = "Persistent Identifier for the asset", example = "asdf-12346-3333-100a21")
    public String asset_pid;
    @Schema(description = "The Global Unique Identifier generated for each asset", example = "ti-a01-202305241657")
    public String asset_guid;
    @Schema(description = "The current status of an asset", example = "BEING_PROCESSED")
    public String status;
    @Schema(description = "A single image (or other type of media) that contains multiple specimens in it. One asset is linked to multiple specimens", example = "false")
    public boolean multi_specimen;
    //@Schema(description = "The barcodes of associated specimens", example = "'[\"ti-sp-00012\"']")
    //public List<String> specimen_barcodes = new ArrayList<>();
    @Schema(description = "A list of specimen objects with the following information: institution, collection, preparation_type, barcode and specimen_pid")
    public List<Specimen> specimens = new ArrayList<>();
    @Schema(description = "A short description of funding source used to create the asset", example = "Hundredetusindvis af dollars")
    public String funding;
    @Schema(description = "We will need to distinguish between image of a folder, device target, specimen, label etc)", example = "folder")
    public String subject;
    @Schema(description = "What the asset represents (image, ct scan, surface scan, document)", example = "ct scan")
    public String payload_type;
    @Schema(description = "The format of the asset", example = "[\"JPEG\"]")
    public List<String> file_formats = new ArrayList<>();
    @Schema(description = "Flags if it is possible to edit / delete the media of this asset", example = "true")
    public boolean asset_locked;
    @Schema(description = "List of possible roles for users", example = "[\"ADMIN\"]")
    public List<InternalRole> restricted_access = new ArrayList<>();

    @Schema(description = "A dictionary of dynamic properties")
    public Map<String, String> tags = new HashMap<>();
    @Schema(description = "Records if the asset has been manually audited", example = "false")
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

    @Schema(description = "Name of the parent media (in most cases, the same as original_parent_name, it can be different if it is a derivative of a derivative)", example = "")
    public String parent_guid;
    @Schema(description = "The collection name within the institution that holds the specimen", example = "test-collection")
    public String collection;
    @Schema(description = "The location on the storage where asset media can be uploaded")
    public HttpInfo httpInfo;
    @Schema(description = "An internal status field used to track the status of the upload of related media", example = "COMPLETED")
    public InternalStatus internal_status;
    @Schema(description = "Username of the person that updated the asset", example = "THBO")
    public String updateUser;
    @Schema(description = "List of the events associated with an asset")
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
    @Schema(description = "The name of the event for the Asset", example = "CREATE_ASSET_METADATA")
    public DasscoEvent event_name;
    @Schema(description = "Whether the current user has write access. Used in frontend operations.", example = "TRUE")
    public boolean writeAccess;

    public String getAsset_guid() {
        return asset_guid;
    }

    @Override
    public String toString() {
        return "Asset{" +
                "asset_pid='" + asset_pid + '\'' +
                ", asset_guid='" + asset_guid + '\'' +
                ", status=" + status +
                ", multi_specimen=" + multi_specimen +
                ", specimens=" + specimens +
                ", funding='" + funding + '\'' +
                ", subject='" + subject + '\'' +
                ", payload_type='" + payload_type + '\'' +
                ", file_formats=" + file_formats +
                ", asset_locked=" + asset_locked +
                ", restricted_access=" + restricted_access +
                ", tags=" + tags +
                ", audited=" + audited +
                ", created_date=" + created_date +
                ", date_metadata_updated=" + date_metadata_updated +
                ", date_asset_taken=" + date_asset_taken +
                ", date_asset_deleted=" + date_asset_deleted +
                ", date_asset_finalised=" + date_asset_finalised +
                ", date_metadata_taken=" + date_metadata_taken +
                ", institution='" + institution + '\'' +
                ", parent_guid='" + parent_guid + '\'' +
                ", collection='" + collection + '\'' +
                ", httpInfo=" + httpInfo +
                ", internal_status=" + internal_status +
                ", updateUser='" + updateUser + '\'' +
                ", events=" + events +
                ", digitiser='" + digitiser + '\'' +
                ", workstation='" + workstation + '\'' +
                ", pipeline='" + pipeline + '\'' +
                ", error_message='" + error_message + '\'' +
                ", error_timestamp=" + error_timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) { // does NOT compare the creation_date, workstation, pipeline and digitiser (to be able to compare the objects regardless of the Event linked to it)
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Asset asset = (Asset) o;
        return multi_specimen == asset.multi_specimen && asset_locked == asset.asset_locked && audited == asset.audited && Objects.equals(asset_pid, asset.asset_pid) && Objects.equals(asset_guid, asset.asset_guid) && status == asset.status && Objects.equals(specimens, asset.specimens) && Objects.equals(funding, asset.funding) && Objects.equals(subject, asset.subject) && Objects.equals(payload_type, asset.payload_type) && Objects.equals(file_formats, asset.file_formats) && Objects.equals(restricted_access, asset.restricted_access) && Objects.equals(tags, asset.tags) && Objects.equals(date_metadata_updated, asset.date_metadata_updated) && Objects.equals(date_asset_taken, asset.date_asset_taken) && Objects.equals(date_asset_deleted, asset.date_asset_deleted) && Objects.equals(date_asset_finalised, asset.date_asset_finalised) && Objects.equals(date_metadata_taken, asset.date_metadata_taken) && Objects.equals(institution, asset.institution) && Objects.equals(parent_guid, asset.parent_guid) && Objects.equals(collection, asset.collection) && Objects.equals(httpInfo, asset.httpInfo) && internal_status == asset.internal_status && Objects.equals(updateUser, asset.updateUser) && Objects.equals(events, asset.events) && Objects.equals(error_message, asset.error_message) && Objects.equals(error_timestamp, asset.error_timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(asset_pid, asset_guid, status, multi_specimen, specimens, funding, subject, payload_type, file_formats, asset_locked, restricted_access, tags, audited, date_metadata_updated, date_asset_taken, date_asset_deleted, date_asset_finalised, date_metadata_taken, institution, parent_guid, collection, httpInfo, internal_status, updateUser, events, error_message, error_timestamp);
    }
}
