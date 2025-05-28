package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.cache.InstitutionCache;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.InstitutionRoleRestriction;
import dk.northtech.dasscoassetservice.domain.Role;
import dk.northtech.dasscoassetservice.repositories.InstitutionRepository;
import dk.northtech.dasscoassetservice.repositories.RestrictedObjectType;
import dk.northtech.dasscoassetservice.repositories.RoleRepository;
import jakarta.inject.Inject;
import joptsimple.internal.Strings;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InstitutionService {

    private static final Logger logger = LoggerFactory.getLogger(InstitutionService.class);
    private static final String name_regex = "^[a-zA-ZÆØÅæøå ]+$";
    private InstitutionCache institutionCache;

    private Jdbi jdbi;
    boolean initialised = false;
    private final ConcurrentHashMap<String, Institution> institutionMap = new ConcurrentHashMap<>();
    private CacheService cacheService;

    @Inject
    public InstitutionService(Jdbi jdbi, CacheService cacheService,
                              InstitutionCache institutionCache) {
        this.jdbi = jdbi;
        this.cacheService = cacheService;
        this.institutionCache = institutionCache;
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
            HashSet<String> allRoles = new HashSet<>(roleRepository.listRoles());
            if (institution.roleRestrictions() != null) {
                for (Role role : institution.roleRestrictions()) {
                    if (!allRoles.contains(role.name())) {
                        if (Strings.isNullOrEmpty(role.name())) {
                            throw new IllegalArgumentException("Role name cannot be null or empty");
                        }
                        roleRepository.createRole(role.name());
                    }
                }
            }
            /*
            if (repository.findInstitution(institution.name()).isPresent()) {
                throw new IllegalArgumentException("Institute already exists");
            }
             */
            if (getIfExists(institution.name()).isPresent()) {
                throw new IllegalArgumentException("Institute already exists");
            }
//        else if (!institution.name().matches(name_regex)){
//            throw new IllegalArgumentException("Name must be alphanumeric");
//        }
            repository.persistInstitution(institution);
            roleRepository.setRestrictions(RestrictedObjectType.INSTITUTION, institution.roleRestrictions(), institution.name());
            //institutionCache.putInstitutionInCache(institution.name(), institution);
            return h;
        });
        //make sure institution exists in collection service
        this.institutionMap.put(institution.name(), institution);
        this.cacheService.reloadRoles();
        this.cacheService.reloadCollections();
        //this.cache.put(institution.name(),institution);
        return institution;
    }

    public List<Institution> listInstitutions() {
        if (institutionMap.isEmpty()) {
            initInstitutions(false);
        }
        return new ArrayList<>(institutionMap.values());
    }

    public Optional<Institution> getIfExists(String institutionName) {
        if (institutionName == null) {
            return Optional.empty();
        }
        if (this.institutionMap.isEmpty()) {
            // There should always be institution in the db so this shouldn't be a problem
            initInstitutions(false);
        }
        Institution institution = this.institutionMap.get(institutionName);
        if (institution == null) {
            return Optional.empty();
        } else {
            return Optional.of(institution);
        }
    }

    public void initInstitutions(boolean force) {
        synchronized (this) {
            if (!this.initialised || force) {
                jdbi.withHandle(h -> {
                    this.institutionMap.clear();
                    RoleRepository roleRepository = h.attach(RoleRepository.class);
                    InstitutionRepository attach = h.attach(InstitutionRepository.class);
                    List<Institution> institutions = attach.listInstitutions();
                    List<InstitutionRoleRestriction> institutionRoleRestrictions = roleRepository.getInstitutionRoleRestriction();
                    Map<String, Institution> nameInstitution = new HashMap<>();
                    for (Institution institution : institutions) {
                        nameInstitution.put(institution.name(), institution);
                    }
                    for (InstitutionRoleRestriction institutionRoleRestriction : institutionRoleRestrictions) {
                        nameInstitution.get(institutionRoleRestriction.institution_name()).roleRestrictions().add(institutionRoleRestriction.role());

                    }

                    this.institutionMap.putAll(nameInstitution);
                    logger.info("Loaded {} institutions", institutionMap.size());
                    return h;
                });
                this.initialised = true;
            }
        }
    }

    public Institution updateInstitution(Institution institution) {
        jdbi.withHandle(h -> {
            Optional<Institution> existing = getIfExists(institution.name());
            if (existing.isEmpty()) {
                throw new IllegalArgumentException("Institution not found");
            }
            RoleRepository roleRepository = h.attach(RoleRepository.class);
//            roleRepository.removeAllRestrictions(RestrictedObjectType.INSTITUTION, institution.name());
            roleRepository.setRestrictions(RestrictedObjectType.INSTITUTION, institution.roleRestrictions(), institution.name());
            institutionCache.put(institution.name(), institution);
            return h;
        });
        this.institutionMap.put(institution.name(), institution);
        return institution;
    }
}
