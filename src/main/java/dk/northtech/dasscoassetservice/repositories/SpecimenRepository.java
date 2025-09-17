package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.AssetSpecimen;
import dk.northtech.dasscoassetservice.domain.Specimen;
import dk.northtech.dasscoassetservice.repositories.helpers.AssetSpecimenMapper;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.MapTo;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.postgresql.jdbc.PgConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SpecimenRepository extends SqlObject {

    @GetGeneratedKeys
    @SqlUpdate("""
    INSERT INTO specimen(collection_id, specimen_pid, barcode, preparation_types) VALUES (:collection_id, :specimen_pid, :barcode, :preparation_types) RETURNING specimen_id
    """)
    Integer insert_specimen(@BindMethods Specimen specimen);

    @SqlUpdate("""
    INSERT INTO asset_specimen(asset_guid, preparation_type, specimen_id) VALUES (:assetGuid, :preparation_type ,:specimenId)
    """)
    void attachSpecimen(String assetGuid, String preparation_type, Integer specimenId);

    @SqlUpdate("""
    UPDATE asset_specimen
    SET asset_detached = true
    WHERE asset_guid = :assetGuid 
        AND specimen_id = :specimenId
    """)
    void detachSpecimen(String assetGuid, Integer specimenId);

    @SqlUpdate("""
    UPDATE specimen 
    SET preparation_types = :preparation_types
      , barcode = :barcode 
    WHERE specimen_id = :specimen_id
""")
    void updateSpecimen(@BindMethods Specimen specimen);

    String find_specimens_by_asset = """
            SELECT specimen.specimen_id
                , specimen.specimen_pid
                , asset_specimen.asset_specimen_id
                , asset_specimen.asset_guid 
                , asset_specimen.preparation_type AS asset_preparation_type
                , asset_specimen.specify_collection_object_attachment_id
                , asset_specimen.asset_detached
                , collection.collection_name AS collection
                , collection.institution_name AS institution
            FROM specimen
                LEFT JOIN asset_specimen USING(specimen_id)
                LEFT JOIN collection USING(collection_id)
            WHERE asset_guid = :assetGuid
            """;
    default List<AssetSpecimen> findAssetSpecimens(String assetGuid) {
        return withHandle(h -> {
            return h.createQuery(find_specimens_by_asset)
                    .bind("assetGuid", assetGuid)
                    .map(new AssetSpecimenMapper())
                    .list();
        });
    }

    @SqlQuery("""
            SELECT specimen.*
                , false AS asset_detached
                , collection.collection_name AS collection
                , collection.institution_name AS institution
            FROM specimen
                LEFT JOIN collection USING (collection_id)
            WHERE specimen_pid = :pid
            """)
    Optional<Specimen> findSpecimensByPID(String pid);

    @SqlQuery("""  
    SELECT asset_guid FROM asset_specimen
    WHERE specimen_id = :specimenId AND preparation_type NOT IN (<preparationTypes>)
""")
    List<String> getGuidsByPreparationTypeAndSpecimenId(@BindList("preparationTypes")Set<String> preparationTypes, Integer specimenId);

    @SqlQuery("SELECT * FROM preparation_type")
    List<String> listPreparationTypesInternal();

    @SqlUpdate("""
    DELETE FROM asset_specimen 
    WHERE asset_guid = :assetGuid 
        AND specimen_id = :specimenId
    """)
    void deleteAssetSpecimen(String assetGuid, Integer specimenId);

    @SqlUpdate("""
    UPDATE asset_specimen
    SET specify_collection_object_attachment_id = :collectionObjectAttachmentId
        , preparation_type = :preparation_type
    WHERE asset_guid = :assetGuid
        AND specimen_id = :specimenId
    """)
    void updateAssetSpecimen(String assetGuid, Integer specimenId, Long collectionObjectAttachmentId, String preparation_type);
}
