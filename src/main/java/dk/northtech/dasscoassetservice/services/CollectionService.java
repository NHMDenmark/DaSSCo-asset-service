package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Institution;
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
    private final InstitutionService institutionService;

    private Jdbi jdbi;

    @Inject
    public CollectionService(InstitutionService institutionService, Jdbi jdbi) {
        this.institutionService = institutionService;
        this.jdbi = jdbi;
    }

    public Collection persistCollection(Collection collection, String institutionName) {
        if (Objects.isNull(collection)) {
            throw new IllegalArgumentException("POST method requires a body");
        }

        if (Strings.isNullOrEmpty(collection.name())) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        Optional<Institution> ifExists = institutionService.getIfExists(institutionName);
        if (ifExists.isEmpty()) {
            throw new IllegalArgumentException("Institute doesnt exist");
        }
        jdbi.inTransaction(h -> {
            CollectionRepository co = h.attach(CollectionRepository.class);
            Institution institution = ifExists.get();
            if (co.listCollections(institution).contains(collection)) {
                throw new IllegalArgumentException("Collection already exists in this institute");
            }


            Collection col = new Collection(collection.name(), institutionName, collection.roleRestrictions());
            co.persistCollection(col);
            return h;
        });

        return collection;
    }

    public List<Collection> listCollections(Institution institution) {
        Optional<Institution> ifExists = institutionService.getIfExists(institution.name());
        if (ifExists.isEmpty()) {
            throw new IllegalArgumentException("Institute doesnt exist");
        }
        return jdbi.withHandle(h -> {
            CollectionRepository repository = h.attach(CollectionRepository.class);
            return repository.listCollections(institution);
        });
    }

    public Optional<Collection> findCollection(String collectionName) {
        return jdbi.withHandle(handle -> {
            CollectionRepository repository = handle.attach(CollectionRepository.class);
            return repository.findCollection(collectionName);
        });
    }
}
