package dk.northtech.dasscoassetservice.domain;

import java.util.HashMap;
import java.util.Map;

public enum QueryItemField {
    ASSET_GUID("asset_guid", "asset_guid"),
    ASSET_LOCKED("asset_locked", "asset_locked"),
    ASSET_PID("asset_pid", "asset_pid"),
    SUBJECT("subject", "subject"),
    SPECIMENS("specimens", "specimens"),
    COLLECTION("collection", "collection_name"),
    DIGITISER("digitiser", "digitiser_user.username"),
    V2_FEATURE_EXTERNAL_PUBLISHER("V2", "V2"),
    FILE_FORMAT("file_format", "file_formats"),
    FUNDING("funding", "funding"),
    INSTITUTION("institution", "institution_name"),
    MULTI_SPECIMEN("multi_specimen", "multi_specimen"), // TODO update the query to handle it
    PARENT_GUID("parent_guid", "parent_guid"),
    PAYLOAD_TYPE("payload_type", "payload_type"),
    RESTRICTED_ACCESS("restricted_access", "restricted_access"), // TODO how does this one work?
    STATUS("status", "status"),
    WORKSTATION("workstation", "workstation_name"),
    UPDATE_USER("update_user", "update_user"), // TODO should this be deleted?
    PIPELINE("pipeline", "pipeline"), // TODO expand the fields to have extra sql that need to be appended e.g here event.event in (CREATE_ASSET, UPDATE_ASSET)
    INTERNAL_STATUS("internal_status", "internal_status"),
    MAKE_PUBLIC("make_public", "make_public"),
    METADATA_SOURCE("metadata_source", "metadata_source"),
    PUSH_TO_SPECIFY("push_to_specify", "push_to_specify"),
    METADATA_VERSION("metadata_version", "metadata_version"),
    COMPLETE_DIGITISER_LIST("complete_digitiser_list", "complete_digitiser_list"), // FIELD DOES NOT EXIST YET
    CAMERA_SETTING_CONTROL("camera_setting_control", "camera_setting_control"),
    MOS_ID("mos_id", "mos_id"),
    SPECIFY_ATTACHMENT_REMARKS("specify_attachment_remarks", "specify_attachment_remarks"),
    SPECIFY_ATTACHMENT_TITLE("specify_attachment_title", "specify_attachment_title"),
    DATE_ASSET_TAKEN("date_asset_taken", "date_asset_taken"),
    DATE_ASSET_FINALISED("date_asset_finalised", "date_asset_finalised"),
    DATE_METADATA_INGESTED("date_metadata_ingested", "date_metadata_ingested"),
    LEGAL("legal", "legal"), // What field in legality?
    ISSUES("issues", "issues"), // What field in issues?
    DATE_ASSET_CREATED_ARS("date_asset_created_ars", "date_asset_created_ars"),
    DATE_ASSET_UPDATED_ARS("date_asset_updated_ars", "date_asset_updated_ars"),
    DATE_ASSET_DELETED_ARS("date_asset_deleted_ars", "date_asset_deleted_ars"),
    DATE_AUDITED("date_audited", "date_audited"),
    DATE_METADATA_CERATED_ARS("date_metadata_created_ars", "date_metadata_created_ars"),
    DATE_METADATA_UPDATED_ARS("date_metadata_updated_ars", "date_metadata_updated_ars"),
    DATE_PUSHED_TO_SPECIFY("date_pushed_to_specify", "date_pushed_to_specify");

    private final String displayName;
    private final String fieldName;

    QueryItemField(String displayName, String fieldName) {
        this.displayName = displayName;
        this.fieldName = fieldName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public static QueryItemField fromDisplayName(String displayName) {
        return BY_DISPLAY.get(displayName);
    }

    // lookup by field name ("asset_guid")
    public static QueryItemField fromFieldName(String fieldName) {
        return BY_FIELD.get(fieldName);
    }

    private static final Map<String, QueryItemField> BY_DISPLAY = new HashMap<>();
    private static final Map<String, QueryItemField> BY_FIELD = new HashMap<>();

    static {
        for (QueryItemField f : values()) {
            BY_DISPLAY.put(f.displayName, f);
            BY_FIELD.put(f.fieldName, f);
        }
    }
}
