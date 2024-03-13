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
        Agtype collectionName = rs.getObject("collection_name", Agtype.class);
        Agtype pipelineName = rs.getObject("pipeline_name", Agtype.class);
        Agtype status = rs.getObject("status", Agtype.class);
        Agtype fileFormats = rs.getObject("file_formats", Agtype.class);
        Agtype internalStatus = rs.getObject("internal_status", Agtype.class);
        Agtype workstationName = rs.getObject("workstation_name", Agtype.class);
        Agtype restrictedAccess = rs.getObject("restricted_access", Agtype.class);
        Agtype tags = rs.getObject("tags", Agtype.class);

        Agtype assetLocked = rs.getObject("asset_locked", Agtype.class);
        asset.internal_status = InternalStatus.valueOf(internalStatus.getString());

        Agtype specimens = rs.getObject("specimens", Agtype.class);
//        asset.specimens = specimenBarcodes.getList().stream().map(Object::toString).collect(Collectors.toList());

        asset.asset_guid = guid.getString();
        asset.asset_pid = pid.getString();
        asset.status = AssetStatus.valueOf(status.getString());
        asset.file_formats = fileFormats.getList().stream().map(x -> FileFormat.valueOf(x.toString())).collect(Collectors.toList());
        asset.multi_specimen = asset.specimens.size() > 1;
        asset.institution = institutionName.getString();
        asset.collection = collectionName.getString();
        asset.pipeline = pipelineName.getString();
        asset.workstation = workstationName.getString();
        asset.asset_locked = assetLocked.getBoolean();
        asset.restricted_access = restrictedAccess.getList().stream().map(role -> Role.valueOf(role.toString())).collect(Collectors.toList());
        Map<String, String> tagsMap = new HashMap<>();
        tags.getMap().entrySet().forEach(tag -> tagsMap.put(tag.getKey(), tag.getValue() != null ? tag.getValue().toString() : null));
        asset.tags = tagsMap;
        AgtypeList list = specimens.getList();
        asset.specimens = list.stream().map(x -> mapSpecimen((AgtypeMap) x)).collect(Collectors.toList());
        // We will get a null pointer if we try to read a null Agtype from the result. This is a workaround
        rs.getString("user_name");
        if (!rs.wasNull()) {
            Agtype userName = rs.getObject("user_name", Agtype.class);
            asset.digitiser = userName.getString();
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
        rs.getString("date_metadata_taken");
        if (!rs.wasNull()) {
            Agtype dateMetaDataTaken = rs.getObject("date_metadata_taken", Agtype.class);
            asset.date_asset_finalised = Instant.ofEpochMilli(dateMetaDataTaken.getLong());
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
        rs.getString("funding");
        if (!rs.wasNull()) {
            Agtype funding = rs.getObject("funding", Agtype.class);
            asset.funding = funding.getString();
        }
        rs.getString("parent_guid");
        if (!rs.wasNull()) {
            Agtype parent_guid = rs.getObject("parent_guid", Agtype.class);
            asset.parent_guid = parent_guid.getString();
        }
        return asset;
    }

    Specimen mapSpecimen(AgtypeMap agtype) {
        AgtypeMap properties = agtype.getMap("properties");
        return new Specimen(properties.getString("specimen_barcode"), properties.getString("specimen_pid"), properties.getString("preparation_type"));
    }
}
