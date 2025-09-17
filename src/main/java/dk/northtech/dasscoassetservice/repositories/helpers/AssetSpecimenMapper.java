package dk.northtech.dasscoassetservice.repositories.helpers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dk.northtech.dasscoassetservice.domain.*;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;

public class AssetSpecimenMapper implements RowMapper<AssetSpecimen> {

    @Override
    public AssetSpecimen map(ResultSet rs, StatementContext ctx) throws SQLException {
        Long specify_id = rs.getLong("specify_collection_object_attachment_id");
        if(rs.wasNull()) {
            specify_id = null;
        }
        AssetSpecimen assetSpecimen = new AssetSpecimen(
                rs.getBoolean("asset_detached")
                , specify_id
                , rs.getString("asset_preparation_type")
                , rs.getLong("asset_specimen_id")
                , rs.getString("specimen_pid")
                , rs.getString("asset_guid")
                , rs.getInt("specimen_id")
        );
        return assetSpecimen;
    }
}
