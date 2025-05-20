package dk.northtech.dasscoassetservice.services;

import com.google.common.base.Strings;
import dk.northtech.dasscoassetservice.cache.PreparationTypeCache;
import dk.northtech.dasscoassetservice.domain.Specimen;
import dk.northtech.dasscoassetservice.repositories.SpecimenRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SpecimenService {
    private final Jdbi jdbi;
    private static final Logger log = LoggerFactory.getLogger(SpecimenService.class);

    private PreparationTypeCache preparationTypeCache;
    private ExtendableEnumService extendableEnumService;

    @Inject
    public SpecimenService(Jdbi jdbi, PreparationTypeCache preparationTypeCache, ExtendableEnumService extendableEnumService) {
        this.jdbi = jdbi;
        this.preparationTypeCache = preparationTypeCache;
        this.extendableEnumService = extendableEnumService;
    }



    public Specimen updateSpecimen(Specimen specimen) {
        Optional<Specimen> specimensByPID = jdbi.onDemand(SpecimenRepository.class).findSpecimensByPID(specimen.specimen_pid());
        if(specimensByPID.isEmpty()) {
            throw new IllegalArgumentException("Specimen was not found");
        }
        validateSpecimen(specimen);
        Specimen existing = specimensByPID.get();
        jdbi.onDemand(SpecimenRepository.class)
                .updateSpecimen(new Specimen(existing.institution()
                        , existing.collection()
                        , specimen.barcode()
                        , specimen.specimen_pid()
                        , specimen.preparation_types()
                        ,null
                        , existing.specimen_id()
                        , existing.collection_id()));
        
        return specimen;
    }

    public List<String> listPreparationTypes(){
        return preparationTypeCache.getPreparationTypes();
    }

    void validateSpecimen(Specimen specimen) {
        if (Strings.isNullOrEmpty(specimen.specimen_pid())) {
            throw new IllegalArgumentException("specimen_pid cannot be null or empty");
        }
        if (Strings.isNullOrEmpty(specimen.barcode())) {
            throw new IllegalArgumentException("Specimen barcode cannot be null");
        }
        if (specimen.preparation_types() == null || specimen.preparation_types().isEmpty()) {
            throw new IllegalArgumentException("a specimen must have at least one preparation_type");
        }
        if(specimen.asset_preparation_type() != null && !specimen.preparation_types().contains(specimen.asset_preparation_type())) {
            throw new IllegalArgumentException("Asset preparation_type is not present in preparation types on this specimen");
        }
        for (String p : specimen.preparation_types()) {
            if (!extendableEnumService.checkExists(ExtendableEnumService.ExtendableEnum.PREPARATION_TYPE, p)) {
                throw new IllegalArgumentException(p + " is not a valid preparation_type");
            }
        }
    }
}
