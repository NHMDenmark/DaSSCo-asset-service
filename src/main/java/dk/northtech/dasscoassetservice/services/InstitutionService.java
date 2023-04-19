package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Institute;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.repositories.InstitutionRepository;
import jakarta.inject.Inject;
import joptsimple.internal.Strings;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.jackson2.Jackson2Plugin;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InstitutionService {

    private InstitutionRepository institutionRepository;
    private static final String name_regex ="^[a-zA-ZÆØÅæøå ]+$";

    @Inject
    public InstitutionService(InstitutionRepository institutionRepository) {
        this.institutionRepository = institutionRepository;
        //        this.jdbi = Jdbi.create(dataSource)
//                .registerRowMapper((ConstructorMapper.factory(Institute.class)))
//                .installPlugin(new Jackson2Plugin());

    }

    public Institution createInstitution(Institution institution) {
        if(Strings.isNullOrEmpty(institution.name())) {
            throw new IllegalArgumentException("Name is cannot be null or empty");
        } else if (!institution.name().matches(name_regex)){
            throw new IllegalArgumentException("Name must be alphanumeric");
        }
        institutionRepository.persistInstitution(institution);
        return institution;
    }

    public List<Institution> listInstitutions() {
//        if(Strings.isNullOrEmpty(institution.name())) {
//            throw new IllegalArgumentException("Name is cannot be null or empty");
//        } else if (!institution.name().matches(name_regex)){
//            throw new IllegalArgumentException("Name must be alphanumeric");
//        }
        return institutionRepository.listInstitutions();
    }

    public Optional<Institution> getIfExists(String institutionName) {
        return institutionRepository.findInstitution(institutionName);
    }
}
