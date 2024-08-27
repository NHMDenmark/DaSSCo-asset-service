package dk.northtech.dasscoassetservice.repositories;

import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.util.Optional;

public interface MappingRepository {

    @Transaction
    default int addInstitutionMapping(String specifyName){
        Optional<Integer> institution = findInstitution(specifyName);
        return institution.orElseGet(() -> addInstitution(specifyName));
    }

    @Transaction
    default int addArsInstitutionMapping(String arsName){
        Optional<Integer> institution = findArsInstitution(arsName);
        return institution.orElseGet(() -> addArsInstitution(arsName));
    }

    @Transaction
    default void addMapping(int institutionId, int arsId){
        Optional<Integer> mapping = findMapping(institutionId, arsId);
        mapping.orElseGet(() -> addSpecifyArsMapping(institutionId, arsId));
    }

    @Transaction
    default String getArsInstitutionMapping(String institution){
        return getSpecifyInstitution(institution);
    }

    @SqlQuery("SELECT id FROM mappings.institutions_specify WHERE name = ?")
    Optional<Integer> findInstitution(String specifyName);

    @SqlQuery("SELECT id FROM mappings.institutions_ars WHERE name = ?")
    Optional<Integer> findArsInstitution(String arsName);

    @SqlQuery("SELECT id FROM mappings.institutions_mapping WHERE institution_specify_id = ? AND institution_ars_id = ?")
    Optional<Integer> findMapping(int institutionId, int arsId);

    @SqlQuery("SELECT spec.name " +
            "FROM mappings.institutions_specify spec " +
            "JOIN mappings.institutions_mapping map ON spec.id = map.institution_specify_id " +
            "JOIN mappings.institutions_ars ars ON map.institution_ars_id = ars.id " +
            "WHERE ars.name = ?")
    String getSpecifyInstitution(String arsInstitution);

    @SqlUpdate("INSERT INTO mappings.institutions_specify (name) VALUES (?)")
    @GetGeneratedKeys
    int addInstitution(String specifyName);

    @SqlUpdate("INSERT INTO mappings.institutions_ars (name) VALUES (?)")
    @GetGeneratedKeys
    int addArsInstitution(String arsName);

    @SqlUpdate("INSERT INTO mappings.institutions_mapping (institution_specify_id, institution_ars_id) VALUES (?, ?)")
    @GetGeneratedKeys
    int addSpecifyArsMapping(int institutionId, int arsId);

    @SqlUpdate("UPDATE mappings.institutions_specify SET name = ? WHERE id = ?")
    void updateSpecifyInstitutionName(String name, Integer id);

    @SqlUpdate("UPDATE mappings.institutions_ars SET name = ? WHERE id = ?")
    void updateArsInstitutionName(String name, Integer id);
}
