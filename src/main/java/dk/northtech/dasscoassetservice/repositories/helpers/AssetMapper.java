package dk.northtech.dasscoassetservice.repositories.helpers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dk.northtech.dasscoassetservice.domain.*;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.type.AgtypeList;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class AssetMapper implements RowMapper<Asset> {
    private static Gson gson = new Gson();
    @Override
    public Asset map(ResultSet rs, StatementContext ctx) throws SQLException {
        Asset asset = new Asset();

        // Mapping each column from the ResultSet to the Asset object
        asset.asset_guid = rs.getString("asset_guid");
        asset.asset_locked = rs.getBoolean("asset_locked");
        asset.subject = rs.getString("subject");
        asset.collection_id = rs.getInt("collection_id");
        asset.digitiser_id = rs.getInt("digitiser_id");
        asset.digitiser = rs.getString("digitiser");
        asset.collection = rs.getString("collection_name");
        asset.workstation = rs.getString("workstation_name");
        if(rs.wasNull()) {
            asset.digitiser_id = null;
        }
        asset.file_format = rs.getString("file_format");
        asset.payload_type = rs.getString("payload_type");
        asset.status = rs.getString("status");

        // Assuming 'tags' is stored as a JSON string in the database, it can be mapped to a String or a Map
        String tagsJson = rs.getString("tags");
        if (tagsJson != null) {
            // Use Gson to deserialize the JSON string into a HashMap
            asset.tags = gson.fromJson(tagsJson, new TypeToken<HashMap<String, String>>() {}.getType());
        }

        asset.workstation_id = rs.getInt("workstation_id");
        if(rs.wasNull()) {
            asset.workstation_id = null;
        }
        asset.institution = rs.getString("institution_name");
        asset.internal_status = InternalStatus.valueOf(rs.getString("internal_status"));
        asset.make_public = rs.getBoolean("make_public");
        asset.metadata_source = rs.getString("metadata_source");
        asset.push_to_specify = rs.getBoolean("push_to_specify");
        asset.metadata_version = rs.getString("metadata_version");
        asset.camera_setting_control = rs.getString("camera_setting_control");

        // Mapping dates (timestamps)
        Timestamp dateAssetTaken = rs.getTimestamp("date_asset_taken");
        if(dateAssetTaken!= null) {
            asset.date_asset_taken = dateAssetTaken.toInstant();

        }
        Timestamp dateAssetFinalised = rs.getTimestamp("date_asset_finalised");
        asset.date_asset_finalised = dateAssetFinalised == null ? null : dateAssetFinalised.toInstant();
        asset.initial_metadata_recorded_by = rs.getString("initial_metadata_recorded_by");
        Timestamp dateMetadataIngested = rs.getTimestamp("date_metadata_ingested");
        asset.date_metadata_ingested = dateMetadataIngested.toInstant();

        return asset;
    }

    Specimen mapSpecimen(AgtypeMap agtype) {
        AgtypeMap properties = agtype.getMap("properties");
        return new Specimen(properties.getString("specimen_barcode"), properties.getString("specimen_pid"), properties.getString("preparation_type"));
    }
}
