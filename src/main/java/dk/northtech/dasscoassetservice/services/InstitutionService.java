package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.repositories.InstitutionRepository;
import dk.northtech.dasscoassetservice.repositories.RestrictedObjectType;
import dk.northtech.dasscoassetservice.repositories.RoleRepository;
import jakarta.inject.Inject;
import joptsimple.internal.Strings;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class InstitutionService {


    private static final String name_regex = "^[a-zA-ZÆØÅæøå ]+$";
    private Jdbi jdbi;

    @Inject
    public InstitutionService(Jdbi jdbi) {
        this.jdbi = jdbi;

    }


    public Institution createInstitution(Institution institution) {

        if (Objects.isNull(institution)) {
            throw new IllegalArgumentException("POST request requires a body");
        }

        if (Strings.isNullOrEmpty(institution.name())) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        jdbi.inTransaction(h -> {
            InstitutionRepository repository = h.attach(InstitutionRepository.class);
            RoleRepository roleRepository = h.attach(RoleRepository.class);
            if (repository.findInstitution(institution.name()).isPresent()) {
                throw new IllegalArgumentException("Institute already exists");
            }
//        else if (!institution.name().matches(name_regex)){
//            throw new IllegalArgumentException("Name must be alphanumeric");
//        }
            repository.persistInstitution(institution);
            roleRepository.setRoleRestriction(RestrictedObjectType.INSTITUTION, institution.name() ,institution.roleRestriction());
            return h;
        });
        return institution;
    }

    public List<Institution> listInstitutions() {
//        if(Strings.isNullOrEmpty(institution.name())) {
//            throw new IllegalArgumentException("Name is cannot be null or empty");
//        } else if (!institution.name().matches(name_regex)){
//            throw new IllegalArgumentException("Name must be alphanumeric");
//        }
        return jdbi.withHandle(h -> {
            InstitutionRepository institutionRepository = h.attach(InstitutionRepository.class);
            return institutionRepository.listInstitutions();
        });
    }

    public Optional<Institution> getIfExists(String institutionName) {
        return jdbi.withHandle(h -> {
            InstitutionRepository institutionRepository = h.attach(InstitutionRepository.class);
            return institutionRepository.findInstitution(institutionName);
        });
    }

    public Institution updateInstitution(Institution institution) {
        jdbi.withHandle(h -> {
            InstitutionRepository institutionRepository = h.attach(InstitutionRepository.class);
            Optional<Institution> existing = institutionRepository.findInstitution(institution.name());
            if (existing.isEmpty()) {
                return null;
            }
            RoleRepository roleRepository = h.attach(RoleRepository.class);
            roleRepository.setRoleRestriction(RestrictedObjectType.INSTITUTION, institution.name(), institution.roleRestriction());
            return h;
        });
        return institution;
    }
}
