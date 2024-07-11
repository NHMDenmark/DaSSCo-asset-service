package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.cache.CollectionCache;
import dk.northtech.dasscoassetservice.cache.InstitutionCache;
import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.User;
import dk.northtech.dasscoassetservice.repositories.CollectionRepository;
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
public class CollectionService {
    private CollectionCache collectionCache;
    private InstitutionCache institutionCache;
    private final InstitutionService institutionService;

    private Jdbi jdbi;

    private RightsValidationService rightsValidationService;

    @Inject
    public CollectionService(InstitutionService institutionService, Jdbi jdbi, RightsValidationService rightsValidationService,
                             InstitutionCache institutionCache,
                             CollectionCache collectionCache) {
        this.institutionService = institutionService;
        this.jdbi = jdbi;
        this.rightsValidationService = rightsValidationService;
        this.collectionCache = collectionCache;
        this.institutionCache = institutionCache;
    }


    public Collection persistCollection(Collection collection) {
        if (Objects.isNull(collection)) {
            throw new IllegalArgumentException("POST method requires a body");
        }

        if (Strings.isNullOrEmpty(collection.name())) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
/*
        Optional<Institution> ifExists = institutionService.getIfExists(collection.institution());
        if (ifExists.isEmpty()) {
            throw new IllegalArgumentException("Institute doesnt exist");
        }

 */
        if (!institutionCache.institutionExists(collection.institution())){
            throw new IllegalArgumentException("Institute doesnt exist");
        } else {
            if (collectionCache.getCollections(collection.institution()).contains(collection)){
                throw new IllegalArgumentException("Collection already exists in this institute");
        }
        jdbi.inTransaction(h -> {
            CollectionRepository co = h.attach(CollectionRepository.class);
            /*
            Institution institution = ifExists.get();
            if (co.listCollections(institution).contains(collection)) {
                throw new IllegalArgumentException("Collection already exists in this institute");
            }
             */
            Collection col = new Collection(collection.name(), collection.institution(), collection.roleRestrictions());
            co.persistCollection(col);
            collectionCache.putCollectionInCache(collection.institution(), col.name(), col);
            return h;
        });
        }

        return collection;
    }

    public List<Collection> listCollections(Institution institution, User user) {
        /*
        Optional<Institution> ifExists = institutionService.getIfExists(institution.name());
        if (ifExists.isEmpty()) {
            throw new IllegalArgumentException("Institute doesnt exist");
        }
         */
        if (institutionCache.institutionExists(institution.name())){
            collectionCache.getCollections(institution.name());
        } else {
            throw new IllegalArgumentException("Institute doesnt exist");
        }
        rightsValidationService.checkReadRightsThrowing(user,institution.name());
        /*
        return jdbi.withHandle(h -> {
            CollectionRepository repository = h.attach(CollectionRepository.class);
            return repository.listCollections(institution);

        });
         */
        return collectionCache.getCollections(institution.name());
    }

    public List<Collection> getAll() {
        return jdbi.withHandle(handle -> {
            CollectionRepository attach = handle.attach(CollectionRepository.class);
            return attach.readAll();
        });
    }
    public Optional<Collection> findCollection(User user, String collectionName, String institutionName) {
        rightsValidationService.checkReadRightsThrowing(user, institutionName,collectionName);
       return findCollectionInternal(collectionName, institutionName);
    }

    public Optional<Collection> findCollectionInternal(String collectionName, String institutionName) {
        return jdbi.withHandle(handle -> {
            CollectionRepository repository = handle.attach(CollectionRepository.class);
            return repository.findCollection(collectionName, institutionName);
        });
    }

    public Collection updateCollection(Collection collection) {
        jdbi.withHandle(h -> {
            RoleRepository roleRepository = h.attach(RoleRepository.class);
            roleRepository.setRoleRestriction(RestrictedObjectType.COLLECTION,collection.name(),collection.roleRestrictions());
            return h;
        });
        collectionCache.putCollectionInCache(collection.institution(), collection.name(), collection);
        return collection;
    }
}
