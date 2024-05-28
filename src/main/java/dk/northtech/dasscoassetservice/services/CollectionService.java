package dk.northtech.dasscoassetservice.services;

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
    private final InstitutionService institutionService;
    private final CollectionRepository collectionRepository;


    @Inject
    public CollectionService(InstitutionService institutionService, CollectionRepository collectionRepository) {
        this.institutionService = institutionService;
        this.collectionRepository = collectionRepository;
    }

    public Collection persistCollection(Collection collection, String institutionName) {
        if (Objects.isNull(collection)){
            throw new IllegalArgumentException("POST method requires a body");
        }

        if (Strings.isNullOrEmpty(collection.name())){
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        Optional<Institution> ifExists = institutionService.getIfExists(institutionName);
        if(ifExists.isEmpty()){
            throw new IllegalArgumentException("Institute doesnt exist");
        } else {
            Institution institution = ifExists.get();
            if (collectionRepository.listCollections(institution).contains(collection)){
                throw new IllegalArgumentException("Collection already exists in this institute");
            }
        }

        Collection col = new Collection(collection.name(), institutionName);

        collectionRepository.persistCollection(col);

        return collection;
    }

    public List<Collection> listCollections(Institution institution) {
        Optional<Institution> ifExists = institutionService.getIfExists(institution.name());
        if(ifExists.isEmpty()){
            throw new IllegalArgumentException("Institute doesnt exist");
        }
        return collectionRepository.listCollections(institution);
    }

    public Optional<Collection> findCollection(String collectionName) {
        return this.collectionRepository.findCollection(collectionName);
    }
}
