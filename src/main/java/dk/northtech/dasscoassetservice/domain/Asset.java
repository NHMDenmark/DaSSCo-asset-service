package dk.northtech.dasscoassetservice.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Map;

public class Asset {
    public Long id;
    public String label;
    @Nullable
    public String notes;
    @Nullable
    public String barcode;
    @Nullable
    public String funding;
    @Nullable
    public String typeStatus;
    @Nullable
    public String accessLevel;
    @Nullable
    public String embargoType;
    @Nullable
    public String specimenPid;
    @Nullable
    public String embargoNotes;
    @Nullable
    public String externalLink;
    public String mediaSubject;
    public String specifySpecimenId;
    @Nullable
    public String otherMultiSpecimen;
    public String multiSpecimenStatus;
    public String pushAssetToSpecify;
    public String specifyAttachmentId;
    public String pushMetadataToSpecify;
    @Nullable
    public String specimanStorageLocation;
    public String originalSpecifyMediaName;

    @JsonProperty("properties")
    private void unpackNested(Map<String,Object> properties) {
        this.notes = (String) properties.get("notes");
        this.barcode = (String) properties.get("barcode");
        this.funding = (String) properties.get("funding");
        this.typeStatus = (String) properties.get("type_status");
        this.accessLevel = (String) properties.get("access_level");
        this.embargoType = (String) properties.get("embargo_type");
        this.specimenPid = (String) properties.get("specimen_pid");
        this.embargoNotes = (String) properties.get("embargo_notes");
        this.externalLink = (String) properties.get("external_link");
        this.mediaSubject = (String) properties.get("media_subject");
        this.specifySpecimenId = (String) properties.get("specify_specimen_id");
        this.otherMultiSpecimen = (String) properties.get("other_nulti_specimen");
        this.multiSpecimenStatus = (String) properties.get("multi_specimen_status");
        this.pushAssetToSpecify = (String) properties.get("push_asset_to_specify");
        this.specifyAttachmentId = (String) properties.get("specify_attachment_id");
        this.pushMetadataToSpecify = (String) properties.get("push_metadata_to_specify");
        this.specimanStorageLocation = (String) properties.get("speciman_storage_location");
        this.originalSpecifyMediaName = (String) properties.get("original_specify_media_name");
    }

    public Asset(Long id, String label, @Nullable String notes, @Nullable String barcode, @Nullable String funding, @Nullable String typeStatus, @Nullable String accessLevel, @Nullable String embargoType, @Nullable String specimenPid, @Nullable String embargoNotes, @Nullable String externalLink, String mediaSubject, String specifySpecimenId, @Nullable String otherMultiSpecimen, String multiSpecimenStatus, String pushAssetToSpecify, String specifyAttachmentId, String pushMetadataToSpecify, @Nullable String specimanStorageLocation, String originalSpecifyMediaName) {
        this.id = id;
        this.label = label;
        this.notes = notes;
        this.barcode = barcode;
        this.funding = funding;
        this.typeStatus = typeStatus;
        this.accessLevel = accessLevel;
        this.embargoType = embargoType;
        this.specimenPid = specimenPid;
        this.embargoNotes = embargoNotes;
        this.externalLink = externalLink;
        this.mediaSubject = mediaSubject;
        this.specifySpecimenId = specifySpecimenId;
        this.otherMultiSpecimen = otherMultiSpecimen;
        this.multiSpecimenStatus = multiSpecimenStatus;
        this.pushAssetToSpecify = pushAssetToSpecify;
        this.specifyAttachmentId = specifyAttachmentId;
        this.pushMetadataToSpecify = pushMetadataToSpecify;
        this.specimanStorageLocation = specimanStorageLocation;
        this.originalSpecifyMediaName = originalSpecifyMediaName;
    }

    @Override
    public String toString() {
        return "Asset{" +
                "id=" + id +
                ", label='" + label + '\'' +
                ", notes='" + notes + '\'' +
                ", barcode='" + barcode + '\'' +
                ", funding='" + funding + '\'' +
                ", typeStatus='" + typeStatus + '\'' +
                ", accessLevel='" + accessLevel + '\'' +
                ", embargoType='" + embargoType + '\'' +
                ", specimenPid='" + specimenPid + '\'' +
                ", embargoNotes='" + embargoNotes + '\'' +
                ", externalLink='" + externalLink + '\'' +
                ", mediaSubject='" + mediaSubject + '\'' +
                ", specifySpecimenId='" + specifySpecimenId + '\'' +
                ", otherMultiSpecimen='" + otherMultiSpecimen + '\'' +
                ", multiSpecimenStatus='" + multiSpecimenStatus + '\'' +
                ", pushAssetToSpecify='" + pushAssetToSpecify + '\'' +
                ", specifyAttachmentId='" + specifyAttachmentId + '\'' +
                ", pushMetadataToSpecify='" + pushMetadataToSpecify + '\'' +
                ", specimanStorageLocation='" + specimanStorageLocation + '\'' +
                ", originalSpecifyMediaName='" + originalSpecifyMediaName + '\'' +
                '}';
    }
}
