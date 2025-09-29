package dk.northtech.dasscoassetservice.domain;

import java.util.HashMap;
import java.util.Map;

public enum QueryItemField {
    ASSET_CERATED_BY("asset_created_by", "event_user.username", "event"),
    ASSET_DELETED_BY("asset_deleted_by", "pipeline_name", "event"),
    ASSET_UPDATED_BY("asset_updated_by", "event_user.username", "event"),
    ASSET_GUID("asset_guid", "asset_guid", "asset"),
    ASSET_LOCKED("asset_locked", "asset_locked", "asset"),
    ASSET_PID("asset_pid", "asset_pid", "asset"),
    AUDITED("audited", "audited", "event"),
    AUDITED_BY("audited_by", "event_user.username", "event"),
    CAMERA_SETTING_CONTROL("camera_setting_control", "camera_setting_control", "asset"),
    COLLECTION("collection", "collection_name", "collection"),
    COMPLETE_DIGITISER_LIST("complete_digitiser_list", "complete_digitiser_list", ""), // FIELD DOES NOT EXIST YET,
    DATE_ASSET_CREATED_ARS("date_asset_created_ars", "date_asset_created_ars", "event"),
    DATE_ASSET_DELETED_ARS("date_asset_deleted_ars", "date_asset_deleted_ars", "event"),
    DATE_ASSET_FINALISED("date_asset_finalised", "date_asset_finalised", "asset"),
    DATE_ASSET_TAKEN("date_asset_taken", "date_asset_taken", "asset"),
    DATE_ASSET_UPDATED_ARS("date_asset_updated_ars", "date_asset_updated_ars", "event"),
    DATE_AUDITED("date_audited", "date_audited", "event"),
    DATE_METADATA_CERATED_ARS("date_metadata_created_ars", "date_metadata_created_ars", "event"),
    DATE_METADATA_INGESTED("date_metadata_ingested", "date_metadata_ingested", "asset"),
    DATE_METADATA_UPDATED_ARS("date_metadata_updated_ars", "date_metadata_updated_ars", "event"),
    DATE_PUSHED_TO_SPECIFY("date_pushed_to_specify", "date_pushed_to_specify", "event"),
    DIGITISER("digitiser", "digitiser_user.username", "digitiser_user"),
    FILE_FORMAT("file_format", "file_formats", "asset"),
    FUNDING("funding", "funding", "funding"),
    INSTITUTION("institution", "institution_name", "collection"),
    INTERNAL_STATUS("internal_status", "internal_status", "asset"),
    ISSUES("issues", "issue.name", "issue"), // What field in issues?
    LEGAL("legal", "copyright", "legality"),
    MAKE_PUBLIC("make_public", "make_public", "asset"),
    METADATA_SOURCE("metadata_source", "metadata_source", "asset"),
    METADATA_CERATED_BY("metadata_created_by", "event_user.username", "event"),
    METADATA_UPDATED_BY("metadata_updated_by", "event_user.username", "event"),
    METADATA_VERSION("metadata_version", "metadata_version", "asset"),
    MOS_ID("mos_id", "mos_id", "asset"),
    MULTI_SPECIMEN("multi_specimen", "specimens.count", ""), // TODO update the query to handle it
    PARENT_GUID("parent_guid", "parent_guid", "parent_child"),
    PAYLOAD_TYPE("payload_type", "payload_type", "asset"),
    PIPELINE("pipeline", "pipeline", "pipeline"), // TODO expand the fields to have extra sql that need to be appended e.g here event.event in (CREATE_ASSET, UPDATE_ASSET)
    PUSH_TO_SPECIFY("push_to_specify", "push_to_specify", "asset"),
    RESTRICTED_ACCESS("restricted_access", "restricted_access", ""), // TODO how does this one work?
    SPECIFY_ATTACHMENT_REMARKS("specify_attachment_remarks", "specify_attachment_remarks", "asset"),
    SPECIFY_ATTACHMENT_TITLE("specify_attachment_title", "specify_attachment_title", "asset"),
    SPECIMENS("specimens", "specimen.specimen_pid", "specimen"),
    STATUS("status", "status", "asset"),
    SUBJECT("subject", "subject", "asset"),
    UPDATE_USER("update_user", "update_user", ""), // TODO should this be deleted?
    V2_FEATURE_EXTERNAL_PUBLISHER("External publisher", "publisher", "publisher"),
    WORKSTATION("workstation", "workstation_name", "workstation");

    private final String displayName;
    private final String fieldName;
    private final String tableName;

    QueryItemField(String displayName, String fieldName, String tableName) {
        this.displayName = displayName;
        this.fieldName = fieldName;
        this.tableName = tableName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getTableName() {return tableName;}

    public static QueryItemField fromDisplayName(String displayName) {
        return BY_DISPLAY.get(displayName);
    }

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
