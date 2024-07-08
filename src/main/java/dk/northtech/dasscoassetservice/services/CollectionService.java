package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.cache.CollectionCache;
import dk.northtech.dasscoassetservice.cache.InstitutionCache;
import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.repositories.CollectionRepository;
import jakarta.inject.Inject;
import joptsimple.internal.Strings;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class CollectionService {
    private final CollectionRepository collectionRepository;
    private CollectionCache collectionCache;
    private InstitutionCache institutionCache;


    @Inject
    public CollectionService(InstitutionCache institutionCache,
                             CollectionRepository collectionRepository, CollectionCache collectionCache) {
        this.collectionRepository = collectionRepository;
        this.collectionCache = collectionCache;
        this.institutionCache = institutionCache;
    }

    public Collection persistCollection(Collection collection, String institutionName) {
        if (Objects.isNull(collection)){
            throw new IllegalArgumentException("POST method requires a body");
        }

        if (Strings.isNullOrEmpty(collection.name())){
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        if (!institutionCache.institutionExists(institutionName)){
            throw new IllegalArgumentException("Institute doesnt exist");
        } else {
            if (collectionCache.getCollections(institutionName).contains(collection)){
                throw new IllegalArgumentException("Collection already exists in this institute");
            }
        }

        Collection col = new Collection(collection.name(), institutionName);

        collectionRepository.persistCollection(col);
        collectionCache.putCollectionInCache(institutionName, col.name(), col);

        return collection;
    }

    public List<Collection> listCollections(Institution institution) {

        if (!institutionCache.getInstitutions().contains(institution)){
            throw new IllegalArgumentException("Institute doesnt exist");
        }
        return collectionCache.getCollections(institution.name());
    }

    public Optional<Collection> findCollection(String collectionName) {
        return this.collectionRepository.findCollection(collectionName);
    }
}
