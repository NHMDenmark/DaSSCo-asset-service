package dk.northtech.dasscoassetservice.repositories.helpers;

import dk.northtech.dasscoassetservice.domain.*;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.type.AgtypeList;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class AssetMapper implements RowMapper<Asset> {

    @Override
    public Asset map(ResultSet rs, StatementContext ctx) throws SQLException {
        Asset asset = new Asset();
        Agtype guid = rs.getObject("asset_guid", Agtype.class);
        Agtype pid = rs.getObject("asset_pid", Agtype.class);
        Agtype institutionName = rs.getObject("institution_name", Agtype.class);
        Agtype status = rs.getObject("status", Agtype.class);
        Agtype fileFormats = rs.getObject("file_formats", Agtype.class);
        Agtype internalStatus = rs.getObject("internal_status", Agtype.class);
        Agtype tags = rs.getObject("tags", Agtype.class);
        Agtype make_public = rs.getObject("make_public", Agtype.class);
        Agtype push_to_specify = rs.getObject("push_to_specify", Agtype.class);
        Agtype assetLocked = rs.getObject("asset_locked", Agtype.class);
        asset.internal_status = InternalStatus.valueOf(internalStatus.getString());

        Agtype specimens = rs.getObject("specimens", Agtype.class);

        asset.asset_guid = guid.getString();
        asset.asset_pid = pid.getString();
        asset.status = status.getString();
        asset.file_formats = fileFormats.getList().stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        asset.multi_specimen = asset.specimens.size() > 1;
        asset.institution = institutionName.getString();
        asset.push_to_specify = push_to_specify.getBoolean();
        asset.make_public = make_public.getBoolean();
        rs.getString("collection_name");
        if (!rs.wasNull()) {
            Agtype collection = rs.getObject("collection_name", Agtype.class);
            asset.collection = collection.getString();
        }
        rs.getString("pipeline_name");
        if (!rs.wasNull()) {
            Agtype pipeline = rs.getObject("pipeline_name", Agtype.class);
            asset.pipeline = pipeline.getString();
        }
        rs.getString("workstation_name");
        if (!rs.wasNull()) {
            Agtype workstation = rs.getObject("workstation_name", Agtype.class);
            asset.workstation = workstation.getString();
        }

        asset.asset_locked = assetLocked.getBoolean();
        Map<String, String> tagsMap = new HashMap<>();
        tags.getMap().entrySet().forEach(tag -> tagsMap.put(tag.getKey(), tag.getValue() != null ? tag.getValue().toString() : null));
        asset.tags = tagsMap;
        AgtypeList list = specimens.getList();
        asset.specimens = list.stream().map(x -> mapSpecimen((AgtypeMap) x)).collect(Collectors.toList());
        // We will get a null pointer if we try to read a null Agtype from the result. This is a workaround
        rs.getString("digitiser");
        if (!rs.wasNull()) {
            Agtype digitiser = rs.getObject("digitiser", Agtype.class);
            asset.digitiser = digitiser.getString();
        }
        rs.getString("creation_date");
        if (!rs.wasNull()) {
            Agtype createdDate = rs.getObject("creation_date", Agtype.class);
            asset.created_date = Instant.ofEpochMilli(createdDate.getLong());
        }
        rs.getString("error_message");
        if (!rs.wasNull()) {
            Agtype errorMessage = rs.getObject("error_message", Agtype.class);
            asset.error_message = errorMessage.getString();
        }
        rs.getString("error_timestamp");
        if (!rs.wasNull()) {
            Agtype dateAssetFinalised = rs.getObject("error_timestamp", Agtype.class);
            asset.error_timestamp = Instant.ofEpochMilli(dateAssetFinalised.getLong());
        }
        rs.getString("date_asset_finalised");
        if (!rs.wasNull()) {
            Agtype dateAssetFinalised = rs.getObject("date_asset_finalised", Agtype.class);
            asset.date_asset_finalised = Instant.ofEpochMilli(dateAssetFinalised.getLong());
        }
        rs.getString("date_metadata_ingested");
        if (!rs.wasNull()) {
            Agtype dateMetaDataTaken = rs.getObject("date_metadata_ingested", Agtype.class);
            asset.date_metadata_ingested = Instant.ofEpochMilli(dateMetaDataTaken.getLong());
        }
        rs.getString("date_asset_taken");
        if (!rs.wasNull()) {
            Agtype assetTakenDate = rs.getObject("date_asset_taken", Agtype.class);
            asset.date_asset_taken = Instant.ofEpochMilli(assetTakenDate.getLong());
        }
        rs.getString("payload_type");
        if (!rs.wasNull()) {
            Agtype assetTakenDate = rs.getObject("payload_type", Agtype.class);
            asset.payload_type = assetTakenDate.getString();
        }
        rs.getString("subject");
        if (!rs.wasNull()) {
            Agtype subject = rs.getObject("subject", Agtype.class);
            asset.subject = subject.getString();
        }

        Agtype funding = rs.getObject("funding", Agtype.class);
        asset.funding = funding.getList().stream()
                .map(f -> new Funding(f.toString()))
                .collect(Collectors.toList());

        Agtype issues = rs.getObject("issues", Agtype.class);
        asset.issues = issues.getList().stream()
                .map(i -> new Issue(i.toString()))
                .collect(Collectors.toList());

        Agtype complete_digitiser_list = rs.getObject("complete_digitiser_list", Agtype.class);
        asset.complete_digitiser_list = complete_digitiser_list.getList().stream()
                .map(Object::toString)
                .collect(Collectors.toList());

        rs.getString("digitiser");
        if (!rs.wasNull()) {
            Agtype digitiser = rs.getObject("digitiser", Agtype.class);
            asset.digitiser = digitiser.getString();
        }

        rs.getString("parent_guid");
        if (!rs.wasNull()) {
            Agtype parent_guid = rs.getObject("parent_guid", Agtype.class);
            asset.parent_guid = parent_guid.getString();
        }
        rs.getString("write_access");
        if (!rs.wasNull()) {
            Agtype writeAccess = rs.getObject("write_access", Agtype.class);
            asset.writeAccess = writeAccess.getBoolean();
        }
//        .camera_setting_control = "Mom get the camera!";
//        asset.date_asset_finalised = Instant.now();
//        asset.metadata_source = "I made it up";
//        asset.metadata_version = "1.0.0";
//        asset.date_metadata_taken = Instant.now();
        rs.getString("camera_setting_control");
        if (!rs.wasNull()) {
            Agtype camera_setting_control = rs.getObject("camera_setting_control", Agtype.class);
            asset.camera_setting_control = camera_setting_control.getString();
        }
        rs.getString("metadata_source");
        if (!rs.wasNull()) {
            Agtype metadata_source = rs.getObject("metadata_source", Agtype.class);
            asset.metadata_source = metadata_source.getString();
        }
        rs.getString("metadata_version");
        if (!rs.wasNull()) {
            Agtype metadata_version = rs.getObject("metadata_version", Agtype.class);
            asset.metadata_version = metadata_version.getString();
        }

        return asset;
    }

    Specimen mapSpecimen(AgtypeMap agtype) {
        AgtypeMap properties = agtype.getMap("properties");
        return new Specimen(properties.getString("specimen_barcode"), properties.getString("specimen_pid"), properties.getString("preparation_type"));
    }
}
