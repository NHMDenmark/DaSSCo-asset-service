package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.domain.Collection;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SpecimenServiceTest extends AbstractIntegrationTest {

    @BeforeEach
    void init() {
        if (user == null) {
            user = userService.ensureExists(new User("TestUzer"));
        }
    }

    User user = null;

    @Test
    void updateSpecimen() {
        Asset updateSpecimen = AssetServiceTest.getTestAsset("updateSpecimen");
        updateSpecimen.specimens = Arrays.asList(new Specimen(updateSpecimen.institution
                , updateSpecimen.collection
                , "updateSpecimen-1"
                , "nhmd.plantz.updateSpecimen-1"
                , new HashSet<>(Set.of("pinning", "slide"))
                ,"pinning"));
        assetService.persistAsset(updateSpecimen, user, 86);
        Optional<Asset> result1 = assetService.getAsset("updateSpecimen");
        Specimen specimen = result1.get().specimens.getFirst();
        assertThat(specimen.preparation_types()).hasSize(2);

        Specimen update = new Specimen(updateSpecimen.institution
                , updateSpecimen.collection
                , "updateSpecimen-x"
                , "nhmd.plantz.updateSpecimen-1"
                , new HashSet<>(Set.of("pinning"))
                , null);

        specimenService.updateSpecimen(update, user);
        Optional<Asset> result2 = assetService.getAsset("updateSpecimen");
        Specimen specimen2 = result2.get().specimens.getFirst();
        assertThat(specimen2.preparation_types().size()).isEqualTo(1);
        assertThat(specimen2.preparation_types()).contains("pinning");
        assertThat(specimen2.barcode()).isEqualTo("updateSpecimen-x");
    }

    @Test
    void updateSpecimenDoNotRemovePrepTypeInUse() {
        Asset updateSpecimen = AssetServiceTest.getTestAsset("updateSpecimenDoNotRemovePrepTypeInUse");
        updateSpecimen.specimens = Arrays.asList(new Specimen(updateSpecimen.institution
                , updateSpecimen.collection
                , "updateSpecimenDoNotRemovePrepTypeInUse-1"
                , "nhmd.plantz.updateSpecimenDoNotRemovePrepTypeInUse-1"
                , new HashSet<>(Set.of("pinning", "slide"))
                ,"pinning"));
        assetService.persistAsset(updateSpecimen, user, 86);


        Specimen update = new Specimen(updateSpecimen.institution
                , updateSpecimen.collection
                , "updateSpecimenDoNotRemovePrepTypeInUse-x"
                , "nhmd.plantz.updateSpecimenDoNotRemovePrepTypeInUse-1"
                , new HashSet<>(Set.of("slide"))
                , null);

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> {
            specimenService.updateSpecimen(update, user);
        });
        assertThat(illegalArgumentException.getMessage()).isEqualTo("Preparation_type cannot be removed as it is used by the following assets: [updateSpecimenDoNotRemovePrepTypeInUse]");
    }


    @Test
    void listPreparationTypes() {
    }

    @Test
    void validateSpecimen() {
        Asset updateSpecimen = AssetServiceTest.getTestAsset("validateSpecimen");
        User restrictedUser = new User("Musketta", Set.of("WRITE_test_1"));
        userService.persistUser(restrictedUser);
        Institution institution = new Institution("testSpecimenInst", Arrays.asList(new Role("test_1")));
        institutionService.createInstitution(institution);
        Collection collection = new Collection("testSpecimenCol", institution.name(), new ArrayList<>());
        collectionService.persistCollection(collection);
        updateSpecimen.specimens = Arrays.asList(new Specimen("testSpecimenInst"
                , "testSpecimenCol"
                , "validateSpecimen-1"
                , "nhmd.plantz.validateSpecimen-1"
                , new HashSet<>(Set.of("pinning", "slide"))
                , "pinning"));
        assetService.persistAsset(updateSpecimen, restrictedUser, 86);

        Specimen specimen = new Specimen("testSpecimenInst"
                , "testSpecimenCol"
                , "validateSpecimen-1"
                , "nhmd.plantz.validateSpecimen-1"
                , new HashSet<>(Set.of("pinning"))
                , null);

        DasscoIllegalActionException dasscoIllegalActionException = assertThrows(DasscoIllegalActionException.class, () -> {
            specimenService.updateSpecimen(specimen, user);
        });
        specimenService.updateSpecimen(specimen, restrictedUser);
    }

}