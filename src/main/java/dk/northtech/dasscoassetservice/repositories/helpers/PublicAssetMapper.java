package dk.northtech.dasscoassetservice.repositories.helpers;

import dk.northtech.dasscoassetservice.domain.Legality;
import dk.northtech.dasscoassetservice.domain.PublicAsset;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PublicAssetMapper implements RowMapper<PublicAsset> {

    @Override
    public PublicAsset map(ResultSet rs, StatementContext ctx) throws SQLException {

        String asset_guid = rs.getString("asset_guid");
        String asset_pid = rs.getString("asset_pid");
        String asset_subject = rs.getString("asset_subject");
        Boolean audited = rs.getBoolean("audited");
        List<String> barcode = getStringArray(rs, "barcode");
        String camera_setting_control = rs.getString("camera_setting_control");
        String collection = rs.getString("collection");
        String date_asset_deleted_ars = rs.getString("date_asset_deleted_ars");
        String date_asset_taken = rs.getString("date_asset_taken");
        String date_audited = rs.getString("date_audited");
        List<String> file_formats = getStringArray(rs, "file_formats");
        List<String> funding = getStringArray(rs, "funding");
        String institution = rs.getString("institution");
        long legality_id = rs.getLong("legality_id");
        if (legality_id > 0) {
        Optional<Legality> legality = Optional.of(new Legality(legality_id, rs.getString("copyright")
                , rs.getString("license")
                , rs.getString("credit")));
        }
        Optional<Legality> legality = Optional.empty();
        String metadata_version = rs.getString("metadata_version");
        List<String> mime_type = getStringArray(rs, "mime_type");
        String mos_id = rs.getString("mos_id");
        Boolean multi_specimen = rs.getBoolean("multi_specimen");
        List<String> parent_guids = getStringArray(rs, "parent_guids");
        String payload_type = rs.getString("payload_type");
        String pipeline_name = rs.getString("pipeline_name");
        List<String> preparation_type = getStringArray(rs, "preparation_type");
        String specify_attachment_title = rs.getString("specify_attachment_title");
        List<String> specimen_pid = getStringArray(rs,"specimen_pids");

        return new PublicAsset(
                asset_guid,
                asset_pid,
                asset_subject,
                audited,
                barcode,
                camera_setting_control,
                collection,
                date_asset_deleted_ars,
                date_asset_taken,
                date_audited,
                file_formats,
                funding,
                institution,
                legality,
                metadata_version,
                mime_type,
                mos_id,
                multi_specimen,
                parent_guids,
                payload_type,
                pipeline_name,
                preparation_type,
                specify_attachment_title,
                specimen_pid
        );
    }

    private static List<String> getStringArray(ResultSet rs, String column) throws SQLException {
        java.sql.Array sqlArray = rs.getArray(column);
        if (sqlArray == null) return List.of();
        String[] arr = (String[]) sqlArray.getArray();
        return Arrays.asList(arr);
    }
}
