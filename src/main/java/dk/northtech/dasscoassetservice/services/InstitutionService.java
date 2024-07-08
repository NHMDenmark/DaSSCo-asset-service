package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.cache.InstitutionCache;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.repositories.InstitutionRepository;
import io.swagger.models.auth.In;
import jakarta.inject.Inject;
import joptsimple.internal.Strings;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class InstitutionService {

    private InstitutionRepository institutionRepository;
    private static final String name_regex ="^[a-zA-ZÆØÅæøå ]+$";
    private InstitutionCache institutionCache;

    @Inject
    public InstitutionService(InstitutionRepository institutionRepository, InstitutionCache institutionCache) {
        this.institutionRepository = institutionRepository;
        //        this.jdbi = Jdbi.create(dataSource)
//                .registerRowMapper((ConstructorMapper.factory(Institute.class)))
//                .installPlugin(new Jackson2Plugin());
        this.institutionCache = institutionCache;
    }

    public Institution createInstitution(Institution institution) {

        if(Objects.isNull(institution)){
            throw new IllegalArgumentException("POST request requires a body");
        }

        if(Strings.isNullOrEmpty(institution.name())) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        if (institutionCache.institutionExists(institution.name())){
            throw new IllegalArgumentException("Institute already exists");
        }
//        else if (!institution.name().matches(name_regex)){
//            throw new IllegalArgumentException("Name must be alphanumeric");
//        }

        institutionRepository.persistInstitution(institution);
        institutionCache.putInstitutionInCache(institution.name(), institution);

        return institution;
    }

    public List<Institution> listInstitutions() {
//        if(Strings.isNullOrEmpty(institution.name())) {
//            throw new IllegalArgumentException("Name is cannot be null or empty");
//        } else if (!institution.name().matches(name_regex)){
//            throw new IllegalArgumentException("Name must be alphanumeric");
//        }
        return institutionCache.getInstitutions().stream().toList();
    }

    public Optional<Institution> getIfExists(String institutionName) {
        return institutionRepository.findInstitution(institutionName);
    }
}
