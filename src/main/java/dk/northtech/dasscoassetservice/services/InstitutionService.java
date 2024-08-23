package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.cache.InstitutionCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
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
    private InstitutionCache institutionCache;

    @Inject
    public InstitutionService(Jdbi jdbi,
                              InstitutionCache institutionCache) {
        this.jdbi = jdbi;
        this.institutionCache = institutionCache;
    }
    LoadingCache<String, Institution> cache = Caffeine.newBuilder()
            .build(x -> {
                return jdbi.withHandle(h -> {
                    InstitutionRepository attach = h.attach(InstitutionRepository.class);
                    Optional<Institution> institution = attach.findInstitution(x);
                    return institution.orElse(null);
                });
            });

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
            /*
            if (repository.findInstitution(institution.name()).isPresent()) {
                throw new IllegalArgumentException("Institute already exists");
            }
             */
            if (institutionCache.institutionExists(institution.name())){
                throw new IllegalArgumentException("Institute already exists");
            }
//        else if (!institution.name().matches(name_regex)){
//            throw new IllegalArgumentException("Name must be alphanumeric");
//        }
            repository.persistInstitution(institution);
            roleRepository.setRoleRestriction(RestrictedObjectType.INSTITUTION, institution.name() ,institution.roleRestriction());
            //institutionCache.putInstitutionInCache(institution.name(), institution);
            return h;
        });
        this.institutionCache.putInstitutionInCacheIfAbsent(institution.name(), institution);
        //this.cache.put(institution.name(),institution);
        return institution;
    }

    public List<Institution> listInstitutions() {
        return institutionCache.getInstitutions();
    }

    public Optional<Institution> getIfExists(String institutionName) {
        if(institutionName == null) {
            return Optional.empty();
        }
        //Institution institution = this.cache.get(institutionName);
        Institution institution = institutionCache.getInstitution(institutionName);
        if(institution == null) {
            return Optional.empty();
        } else  {
            return Optional.of(institution);
        }
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
        institutionCache.putInstitutionInCacheIfAbsent(institution.name(), institution);
        //cache.put(institution.name(),institution);
        return institution;
    }
}
