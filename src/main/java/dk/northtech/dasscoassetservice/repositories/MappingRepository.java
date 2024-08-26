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
    default int addMapping(int institutionId, int arsId){
        Optional<Integer> mapping = findMapping(institutionId, arsId);
        return mapping.orElseGet(() -> addSpecifyArsMapping(institutionId, arsId));
    }

    @SqlQuery("SELECT id FROM mappings.institutions_specify WHERE name = ?")
    Optional<Integer> findInstitution(String specifyName);

    @SqlQuery("SELECT id FROM mappings.institutions_ars WHERE name = ?")
    Optional<Integer> findArsInstitution(String arsName);

    @SqlQuery("SELECT id FROM mappings.institutions_mapping WHERE institution_specify_id = ? AND institution_ars_id = ?")
    Optional<Integer> findMapping(int institutionId, int arsId);

    @SqlUpdate("INSERT INTO mappings.institutions_specify (name) VALUES (?)")
    @GetGeneratedKeys
    int addInstitution(String specifyName);

    @SqlUpdate("INSERT INTO mappings.institutions_ars (name) VALUES (?)")
    @GetGeneratedKeys
    int addArsInstitution(String arsName);

    @SqlUpdate("INSERT INTO mappings.institutions_mapping (institution_specify_id, institution_ars_id) VALUES (?, ?)")
    @GetGeneratedKeys
    int addSpecifyArsMapping(int institutionId, int arsId);
}
