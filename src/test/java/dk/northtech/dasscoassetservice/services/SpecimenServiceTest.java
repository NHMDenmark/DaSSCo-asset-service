package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
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

//    @Test
//    void updateSpecimen() {
//        Specimen specimen = new Specimen()
//    }

    @Test
    void updateSpecimenDoNotRemovePrepTypeInUse() {
        Asset updateSpecimen = AssetServiceTest.getTestAsset("updateSpecimenDoNotRemovePrepTypeInUse");
        roleService.addRole("NHMD");

        Specimen specimen = new Specimen(updateSpecimen.institution
                , updateSpecimen.collection
                , "updateSpecimenDoNotRemovePrepTypeInUse-1"
                , "nhmd.plantz.updateSpecimenDoNotRemovePrepTypeInUse-1"
                , new HashSet<>(Set.of("pinning", "slide"))
                ,null, collectionService.findCollectionInternal(updateSpecimen.collection
                , updateSpecimen.institution).get().collection_id()
                , Arrays.asList(new Role("NHMD")));
        specimenService.putSpecimen(specimen, user);
        updateSpecimen.asset_specimen = Arrays.asList(new AssetSpecimen(updateSpecimen.asset_guid,specimen.specimen_pid(),"pinning",false));
        User nhmd = new User("nhmd-user", Set.of("WRITE_NHMD"));
        User nhmdWithId = userService.ensureExists(nhmd);
        assetService.persistAsset(updateSpecimen, nhmdWithId, 86);
        Optional<Specimen> resultSpecimen = specimenService.findSpecimen("nhmd.plantz.updateSpecimenDoNotRemovePrepTypeInUse-1");
        assertThat(resultSpecimen.isPresent()).isTrue();
        Specimen specimen1 = resultSpecimen.get();
        assertThat(specimen1.role_restrictions()).contains(new Role("NHMD"));

        Specimen update = new Specimen(updateSpecimen.institution
                , updateSpecimen.collection
                , "updateSpecimenDoNotRemovePrepTypeInUse-1"
                , "nhmd.plantz.updateSpecimenDoNotRemovePrepTypeInUse-1"
                , new HashSet<>(Set.of("slide"))
                ,null, collectionService.findCollectionInternal(updateSpecimen.collection
                , updateSpecimen.institution).get().collection_id()
                ,Arrays.asList(new Role("NHMD")));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> {
            specimenService.putSpecimen(update, user);
        });
        assertThat(illegalArgumentException.getMessage()).isEqualTo("Preparation_type cannot be removed as it is used by the following assets: [updateSpecimenDoNotRemovePrepTypeInUse]");
    }

//
//    @Test
//    void listPreparationTypes() {
//    }
//



}