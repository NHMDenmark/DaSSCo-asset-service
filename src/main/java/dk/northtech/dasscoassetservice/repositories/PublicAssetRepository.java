package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.DasscoEvent;
import dk.northtech.dasscoassetservice.domain.Event;
import dk.northtech.dasscoassetservice.domain.PublicAsset;
import dk.northtech.dasscoassetservice.repositories.helpers.EventMapper;
import dk.northtech.dasscoassetservice.repositories.helpers.PublicAssetMapper;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.util.Optional;

public interface PublicAssetRepository extends SqlObject {


    @Transaction
    default Optional<PublicAsset> readPublicAsset(String asset_guid) {
        return withHandle(handle -> {

            return handle.createQuery(
                            """
                                    
                                      SELECT
                                        asset.asset_guid,
                                        asset.asset_pid,
                                        asset.subject AS asset_subject,
                                        asset.metadata_version,
                                        asset.camera_setting_control,
                                        asset.date_asset_taken,
                                        asset.file_formats,
                                        asset.mos_id,
                                        asset.specify_attachment_title,
                                        asset.payload_type,
                                        collection.collection_name AS collection,
                                        collection.institution_name AS institution,
                                        legality.legality_id,
                                        legality.license,
                                        legality.copyright,
                                        legality.credit,
                                        EXISTS (
                                            SELECT 1
                                            FROM event e
                                            WHERE e.asset_guid = asset.asset_guid
                                              AND e.event = :audit_event
                                        ) AS audited,
                                             (
                                                  SELECT e.timestamp
                                                  FROM event e
                                                  WHERE e.asset_guid = asset.asset_guid
                                                    AND e.event = :audit_event
                                                  ORDER BY e.timestamp DESC
                                                  LIMIT 1
                                              ) AS date_audited,
                                        (
                                            SELECT e.timestamp
                                            FROM event e
                                            WHERE e.asset_guid = asset.asset_guid
                                              AND e.event = :asset_deletion
                                            ORDER BY e.timestamp DESC
                                            LIMIT 1
                                        ) AS date_asset_deleted_ars,
                                          (
                                              SELECT p.pipeline_name
                                              FROM event e
                                              LEFT JOIN pipeline p USING (pipeline_id)
                                              WHERE e.asset_guid = asset.asset_guid
                                                AND e.event = :asset_creation
                                              ORDER BY e.timestamp DESC
                                              LIMIT 1
                                          ) AS pipeline_name,
                                        ARRAY_REMOVE(ARRAY_AGG(file.mime_type), NULL) as mime_type,
                                        COUNT(DISTINCT s.specimen_id) > 1 AS multi_specimen,
                                        ARRAY_REMOVE(ARRAY_AGG(DISTINCT s.preparation_type), NULL) AS preparation_type,
                                        ARRAY_REMOVE(ARRAY_AGG(DISTINCT f.funding), NULL) AS funding,
                                        ARRAY_REMOVE(ARRAY_AGG(DISTINCT pc.parent_guid), NULL) AS parent_guids,
                                        ARRAY_REMOVE(ARRAY_AGG(DISTINCT sp.barcode), NULL) AS barcode,
                                        ARRAY_REMOVE(ARRAY_AGG(DISTINCT sp.specimen_pid), NULL) AS specimen_pids
                                    FROM asset
                                    LEFT JOIN collection USING (collection_id)
                                    LEFT JOIN legality USING (legality_id)
                                    LEFT JOIN file USING (asset_guid)
                                    LEFT JOIN asset_specimen s USING (asset_guid)
                                    LEFT JOIN specimen sp ON sp.specimen_id = s.specimen_id
                                    LEFT JOIN asset_funding af USING (asset_guid)
                                    LEFT JOIN funding f USING (funding_id)
                                    LEFT JOIN parent_child pc ON pc.child_guid = asset.asset_guid
                                    WHERE asset.asset_guid = :asset_guid
                                    GROUP BY
                                        asset.asset_guid,
                                        asset.asset_pid,
                                        asset.subject,
                                        asset.metadata_version,
                                        asset.camera_setting_control,
                                        asset.date_asset_taken,
                                        asset.mos_id,
                                        asset.specify_attachment_title,
                                        asset.payload_type,
                                        collection.collection_name,
                                        collection.institution_name,
                                        legality.legality_id,
                                        legality.license,
                                        legality.copyright,
                                        legality.credit;
                                    """
                    )
                    .bind("asset_guid", asset_guid)
                    .bind("audit_event", DasscoEvent.AUDIT_ASSET.name())
                    .bind("asset_deletion", DasscoEvent.DELETE_ASSET_METADATA.name())
                    .bind("asset_creation", DasscoEvent.CREATE_ASSET_METADATA.name())
                    .map(new PublicAssetMapper())
                    .findOne();
        });
    }
}
