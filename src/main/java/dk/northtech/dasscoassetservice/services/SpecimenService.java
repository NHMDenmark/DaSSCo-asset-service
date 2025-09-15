package dk.northtech.dasscoassetservice.services;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Strings;
import dk.northtech.dasscoassetservice.cache.PreparationTypeCache;
import dk.northtech.dasscoassetservice.domain.Specimen;
import dk.northtech.dasscoassetservice.domain.User;
import dk.northtech.dasscoassetservice.repositories.RestrictedObjectType;
import dk.northtech.dasscoassetservice.repositories.RoleRepository;
import dk.northtech.dasscoassetservice.repositories.SpecimenRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class SpecimenService {
    private final Jdbi jdbi;
    private static final Logger log = LoggerFactory.getLogger(SpecimenService.class);

    private PreparationTypeCache preparationTypeCache;
    private ExtendableEnumService extendableEnumService;
    private RightsValidationService rightsValidationService;

    LoadingCache<Integer,List<String>> roleRestrictionCache = Caffeine.newBuilder() // <user, <"read", ["collection2"]>>
            .expireAfterWrite(12, TimeUnit.HOURS)
            .build(specimen_id -> {
                return getRoleRestictions(specimen_id);
            });

    @Inject
    public SpecimenService(Jdbi jdbi, PreparationTypeCache preparationTypeCache, ExtendableEnumService extendableEnumService, RightsValidationService rightsValidationService) {
        this.jdbi = jdbi;
        this.preparationTypeCache = preparationTypeCache;
        this.extendableEnumService = extendableEnumService;
        this.rightsValidationService = rightsValidationService;
    }

    private List<String> getRoleRestictions(Integer specimen_id) {
        RoleRepository roleRepository = jdbi.onDemand(RoleRepository.class);
        return roleRepository.findRoleRestrictions(RestrictedObjectType.SPECIMEN, specimen_id);

    }

    public Specimen getSpecimen(String specimen_pid){

    }

    public Specimen putSpecimen(Specimen specimen, User user) {
        return jdbi.inTransaction(h -> {
            SpecimenRepository specimenRepository = h.attach(SpecimenRepository.class);

            Optional<Specimen> specimensByPID = specimenRepository.findSpecimensByPID(specimen.specimen_pid());
            validateSpecimen(specimen);
            if (specimensByPID.isEmpty()) {
                Integer specimen_id = specimenRepository.insert_specimen(specimen);
                if (!specimen.role_restrictions().isEmpty()) {
                    RoleRepository roleRepository = h.attach(RoleRepository.class);
                    roleRepository.setRestrictions(RestrictedObjectType.SPECIMEN, specimen.role_restrictions(), specimen_id);

                }
                return specimen;
            } else {
                return updateSpecimen(specimen, specimensByPID.get(), user);
            }
        });
    }



    public Specimen updateSpecimen(Specimen specimen, Specimen existing, User user) {
        return jdbi.inTransaction(h -> {
            SpecimenRepository specimenRepository = h.attach(SpecimenRepository.class);

            rightsValidationService.checkWriteRightsThrowing(user, specimen.institution(), specimen.collection());
            List<String> assetsWithRemovedPreparationType = specimenRepository.getGuidsByPreparationTypeAndSpecimenId(specimen.preparation_types(), existing.specimen_id());
            if (specimen.role_restrictions() != null && !existing.role_restrictions().equals(specimen.role_restrictions())) {
                RoleRepository roleRepository = h.attach(RoleRepository.class);
                roleRepository.setRestrictions(RestrictedObjectType.SPECIMEN, specimen.role_restrictions(), existing.specimen_id());
            }
            if (!assetsWithRemovedPreparationType.isEmpty()) {
                String errorMessage = "Preparation_type cannot be removed as it is used by the following assets: " + assetsWithRemovedPreparationType;
                throw new IllegalArgumentException(errorMessage);
            }
            specimenRepository
                    .updateSpecimen(new Specimen(existing.institution()
                            , existing.collection()
                            , specimen.barcode()
                            , specimen.specimen_pid()
                            , specimen.preparation_types()
                            , null
                            , existing.specimen_id()
                            , existing.collection_id()
                            , null
                            , false
                            , specimen.role_restrictions()));

            return specimen;
        });
    }

    public List<String> listPreparationTypes() {
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
        if (specimen.asset_preparation_type() != null && !specimen.preparation_types().contains(specimen.asset_preparation_type())) {
            throw new IllegalArgumentException("Asset preparation_type is not present in preparation types on this specimen");
        }
        for (String p : specimen.preparation_types()) {
            if (!extendableEnumService.checkExists(ExtendableEnumService.ExtendableEnum.PREPARATION_TYPE, p)) {
                throw new IllegalArgumentException(p + " is not a valid preparation_type");
            }
        }
    }

}
