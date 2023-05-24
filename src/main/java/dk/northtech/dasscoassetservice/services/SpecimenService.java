package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Specimen;
import dk.northtech.dasscoassetservice.repositories.SpecimenRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.statement.HashPrefixSqlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;

@Service
public class SpecimenService {
    private final Jdbi jdbi;
    private static final Logger log = LoggerFactory.getLogger(SpecimenService.class);
    @Inject
    public SpecimenService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public Specimen updateSpecimen(Specimen specimen) {
        jdbi.onDemand(SpecimenRepository.class).updateSpecimen(specimen);
        return specimen;
    }
}
