package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.cache.CollectionCache;
import dk.northtech.dasscoassetservice.cache.InstitutionCache;
import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.repositories.CollectionRepository;
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
public class CollectionService {
    private static final Logger logger = LoggerFactory.getLogger(CollectionService.class);
    private final InstitutionService institutionService;
    private boolean initialised;
    private RoleService roleService;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Collection>> collectionCache = new ConcurrentHashMap<>();
    private Jdbi jdbi;

    private RightsValidationService rightsValidationService;

    @Inject
    public CollectionService(InstitutionService institutionService, Jdbi jdbi, RightsValidationService rightsValidationService, RoleService roleService) {
        this.institutionService = institutionService;
        this.jdbi = jdbi;
        this.rightsValidationService = rightsValidationService;
        this.roleService = roleService;
    }


    public Collection persistCollection(Collection collection) {
        if (this.collectionCache.isEmpty()) {
            initCollections();
        }
        if (Objects.isNull(collection)) {
            throw new IllegalArgumentException("POST method requires a body");
        }

        if (Strings.isNullOrEmpty(collection.name())) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        Optional<Institution> ifExists = institutionService.getIfExists(collection.institution());
        if (ifExists.isEmpty()) {
            throw new IllegalArgumentException("Institute doesnt exist");
        }


        if (collectionCache.get(collection.institution()).containsKey(collection.name())) {
            throw new IllegalArgumentException("Collection already exists in this institution");
        }
        jdbi.inTransaction(h -> {
            CollectionRepository co = h.attach(CollectionRepository.class);
            /*
            Institution institution = ifExists.get();
            if (co.listCollections(institution).contains(collection)) {
                throw new IllegalArgumentException("Collection already exists in this institute");
            }
             */
            Collection col = new Collection(collection.name(), collection.institution(), null, collection.roleRestrictions()== null ? new ArrayList<>(): collection.roleRestrictions());
            Collection persistedCollection = co.persistCollection(col);
            RoleRepository roleRepository = h.attach(RoleRepository.class);
            Set<String> roles = roleService.getRoles();
            for (Role role : col.roleRestrictions()) {
                if (!roles.contains(role.name())) {
                    if (Strings.isNullOrEmpty(role.name())) {
                        throw new IllegalArgumentException("Role cannot be null or empty");
                    }
                    roleRepository.createRole(role.name());

                }
            }
            roleRepository.setRestrictions(RestrictedObjectType.COLLECTION, collection.roleRestrictions(), persistedCollection.collection_id());
            // restrictiones arent fetched on creation
            persistedCollection.roleRestrictions().addAll(col.roleRestrictions());;
            this.collectionCache.get(persistedCollection.institution()).put(persistedCollection.name(), persistedCollection);
            return h;
        });
        roleService.initRoles(true);

        return collection;
    }

    public List<Collection> listCollections(Institution institution, User user) {
        if (this.collectionCache.isEmpty()) {
            initCollections();
        }
        if (collectionCache.containsKey(institution.name())) {
            return new ArrayList<>(collectionCache.get(institution.name()).values());
        }
        throw new IllegalArgumentException("Institution does not exist");
    }

    public List<Collection> listCollectionsInternal(Institution institution) {
        if (this.collectionCache.isEmpty()) {
            initCollections();
        }
        if (collectionCache.containsKey(institution.name())) {
            return new ArrayList<>(collectionCache.get(institution.name()).values());
        }
        return null;
    }

    public List<Collection> getAll() {
        return jdbi.withHandle(handle -> {
            CollectionRepository attach = handle.attach(CollectionRepository.class);
            return attach.readAll();
        });
    }

    public Optional<Collection> findCollection(User user, String collectionName, String institutionName) {
        rightsValidationService.checkReadRightsThrowing(user, institutionName, collectionName);
        return findCollectionInternal(collectionName, institutionName);
    }

    public Optional<Collection> findCollectionInternal(String collectionName, String institutionName) {
        if (collectionCache.isEmpty()) {
            initCollections();
        }
        if (collectionCache.containsKey(institutionName)) {
            Collection collection = collectionCache.get(institutionName).get(collectionName);
            if (collection != null) {
                return Optional.of(collection);
            }
        }
        return Optional.empty();
    }

    public Collection updateCollection(Collection collection) {
        Optional<Collection> collectionInternal = findCollectionInternal(collection.name(), collection.institution());
        if (collectionInternal.isEmpty()) {
            throw new IllegalArgumentException("Collection [" + collection.name() + "] was not found in institution [" + collection.institution() + "]");
        }
        Set<String> roles = roleService.getRoles();
        jdbi.withHandle(h -> {
            Collection existing = collectionInternal.get();
            existing.roleRestrictions().clear();
            existing.roleRestrictions().addAll(collection.roleRestrictions());
            RoleRepository roleRepository = h.attach(RoleRepository.class);
            collection.roleRestrictions().forEach(role -> {
                if(!roles.contains(role.name())){
                    roleService.addRole(role.name());
                }
            });
            roleRepository.setRestrictions(RestrictedObjectType.COLLECTION, existing.roleRestrictions(), existing.collection_id());
            return h;
        });
        collectionCache.get(collection.institution()).put(collection.name(), collection);//(collection.institution(), collection.name(), collection);
        return collection;
    }
    public void initCollections() {
        initCollections(false);
    }
    // Load all collections and role restrictions into memory
    public void initCollections(boolean force) {
        synchronized (this) {
            if (!this.initialised || force) {
                jdbi.withHandle(h -> {

                    RoleRepository roleRepository = h.attach(RoleRepository.class);
                    CollectionRepository attach = h.attach(CollectionRepository.class);
                    List<Collection> collections = attach.readAll();
                    HashMap<Integer, Collection> integerCollectionHashMap = new HashMap<>();
                    for (Collection collection : collections) {
                        integerCollectionHashMap.put(collection.collection_id(), collection);
                    }
                    List<CollectionRoleRestriction> collectionRoleRestrictions = roleRepository.getCollectionRoleRestrictions();
                    collectionRoleRestrictions.forEach(crr -> {
                        integerCollectionHashMap.get(crr.collection_id()).roleRestrictions().add(crr.role());
                    });
//                    List<InstitutionRoleRestriction> institutionRoleRestrictions = roleRepository.getInstitutionRoleRestriction();
                    this.collectionCache.clear();
                    this.institutionService.listInstitutions().forEach(i -> {
                        System.out.println("Hej" + i);
                        this.collectionCache.put(i.name(), new ConcurrentHashMap<>());
                    });
                    for (Collection collection : integerCollectionHashMap.values()) {
                        System.out.println(collection);
                        this.collectionCache
                                .get(collection.institution())
                                .put(collection.name(), collection);
                    }

                    logger.info("Loaded {} collections", collectionCache.size());
                    return h;
                });
                this.initialised = true;
            }
        }
    }
}
