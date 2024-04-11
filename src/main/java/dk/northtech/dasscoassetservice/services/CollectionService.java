package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.repositories.CollectionRepository;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

import java.util.List;
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

    public Collection persistCollection(Collection collection) {
        Optional<Institution> ifExists = institutionService.getIfExists(collection.institution());
        if(ifExists.isEmpty()){
            throw new IllegalArgumentException("Institute doesnt exist");
        } else {
            Institution institution = ifExists.get();
            if (collectionRepository.listCollections(institution).contains(collection)){
                throw new IllegalArgumentException("Collection already exists in this institute");
            }
        }
        collectionRepository.persistCollection(collection);
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
