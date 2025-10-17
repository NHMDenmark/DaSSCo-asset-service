package dk.northtech.dasscoassetservice.domain;

import java.util.List;
import java.util.Optional;

public record PublicAsset(
        String asset_guid,
        String asset_pid,
        String asset_subject,
        Boolean audited,
        List<String> barcode,
        String camera_setting_control,
        String collection,
        String date_asset_deleted_ars,
        String date_asset_taken,
        String date_audited,
        List<String> file_formats,
        List<String> funding,
        String institution,
        Optional<Legality> legality,
        String metadata_version,
        List<String> mime_type,
        String mos_id,
        Boolean multi_specimen,
        List<String> parent_guids,
        String payload_type,
        String pipeline_name,
        List<String> preparation_type,
        String specify_attachment_title,
        List<String> specimen_pid
) {
}
