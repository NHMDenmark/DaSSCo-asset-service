package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.Specimen;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.sqlobject.SqlObject;
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
    DELETE FROM asset_specimen 
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

    @SqlQuery("""
            SELECT specimen.*
                , asset_specimen.preparation_type AS asset_preparation_type
                , collection.collection_name AS collection
                , collection.institution_name AS institution
            FROM specimen
                LEFT JOIN asset_specimen USING(specimen_id)
                LEFT JOIN collection USING (collection_id)
            WHERE asset_guid = :assetGuid
            """)
    List<Specimen> findSpecimensByAsset(String assetGuid);

    @SqlQuery("""
            SELECT specimen.*
                , NULL as asset_preparation_type
                , collection.collection_name AS collection
                , collection.institution_name AS institution
            FROM specimen
                LEFT JOIN collection USING (collection_id)
            WHERE specimen_pid = :pid
            """)
    Optional<Specimen> findSpecimensByPID(String pid);


    @SqlQuery("SELECT * FROM preparation_type")
    List<String> listPreparationTypesInternal();

}
