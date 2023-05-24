package dk.northtech.dasscoassetservice.repositories.helpers;

import dk.northtech.dasscoassetservice.domain.*;
import org.apache.age.jdbc.base.Agtype;
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
        Agtype guid = rs.getObject("guid", Agtype.class);
        Agtype pid = rs.getObject("pid", Agtype.class);
        Agtype institutionName = rs.getObject("institution_name", Agtype.class);
        Agtype collectionName = rs.getObject("collection_name", Agtype.class);
        Agtype pipelineName = rs.getObject("pipeline_name", Agtype.class);
        Agtype status = rs.getObject("status", Agtype.class);
        Agtype fileFormats = rs.getObject("file_formats", Agtype.class);
        Agtype internalStatus = rs.getObject("internal_status", Agtype.class);
        Agtype specimenBarcodes = rs.getObject("specimen_barcodes", Agtype.class);
        Agtype workstationName = rs.getObject("workstation_name", Agtype.class);
        Agtype restrictedAccess = rs.getObject("restricted_access", Agtype.class);
        Agtype tags = rs.getObject("tags", Agtype.class);
        Agtype userName = rs.getObject("user_name", Agtype.class);
        Agtype assetLocked = rs.getObject("asset_locked", Agtype.class);
        asset.internal_status = InternalStatus.valueOf(internalStatus.getString());
        asset.specimen_barcodes = specimenBarcodes.getList().stream().map(Object::toString).collect(Collectors.toList());
        asset.guid = guid.getString();
        asset.pid = pid.getString();
        asset.status = AssetStatus.valueOf(status.getString());
        asset.file_formats = fileFormats.getList().stream().map(x -> FileFormat.valueOf(x.toString())).collect(Collectors.toList());
        asset.multi_specimen = asset.specimen_barcodes.size() > 1;
        asset.institution = institutionName.getString();
        asset.collection = collectionName.getString();
        asset.pipeline = pipelineName.getString();
        asset.workstation = workstationName.getString();
        asset.asset_locked = assetLocked.getBoolean();
        asset.digitizer = userName.getString();
        asset.asset_location = "/" + asset.institution + "/" + asset.collection + "/" + asset.guid;
        asset.restricted_access = restrictedAccess.getList().stream().map(role -> Role.valueOf(role.toString())).collect(Collectors.toList());
        Map<String, String> tagsMap = new HashMap<>();
        tags.getMap().entrySet().forEach(tag -> tagsMap.put(tag.getKey(), tag.getValue() != null? tag.getValue().toString(): null));
        asset.tags = tagsMap;
        // We will get a null pointer if we try to read a null Agtype from the result. This is a workaround
        rs.getString("pushed_to_specify_date");
        if (!rs.wasNull()) {
            Agtype pushedToSpecifyDate = rs.getObject("pushed_to_specify_date", Agtype.class);
            asset.pushed_to_specify_date = Instant.ofEpochMilli(pushedToSpecifyDate.getLong());
        }
        rs.getString("asset_taken_date");
        if (!rs.wasNull()) {
            Agtype assetTakenDate = rs.getObject("asset_taken_date", Agtype.class);
            asset.asset_taken_date = Instant.ofEpochMilli(assetTakenDate.getLong());
        }
        rs.getString("payload_type");
        if (!rs.wasNull()) {
            Agtype assetTakenDate = rs.getObject("payload_type", Agtype.class);
            asset.payload_type = assetTakenDate.getString();
        }
        rs.getString("subject");
        if(!rs.wasNull()) {
            Agtype subject = rs.getObject("subject", Agtype.class);
            asset.subject = subject.getString();
        }
        rs.getString("funding");
        if(!rs.wasNull()) {
            Agtype funding = rs.getObject("funding", Agtype.class);
            asset.funding = funding.getString();
        }
        rs.getString("parent_guid");
        if(!rs.wasNull()) {
            Agtype parent_guid = rs.getObject("parent_guid", Agtype.class);
            asset.parent_guid = parent_guid.getString();
        }
        return asset;
    }
}
