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
        User nhmd = new User("nhmd-user", Set.of("WRITE_NHMD"));
        User nhmdWithId = userService.ensureExists(nhmd);

        Specimen specimen = new Specimen(updateSpecimen.institution
                , updateSpecimen.collection
                , "updateSpecimenDoNotRemovePrepTypeInUse-1"
                , "nhmd.plantz.updateSpecimenDoNotRemovePrepTypeInUse-1"
                , new HashSet<>(Set.of("pinning", "slide"))
                ,null, collectionService.findCollectionInternal(updateSpecimen.collection
                , updateSpecimen.institution).get().collection_id()
                , Arrays.asList(new Role("NHMD")));
        specimenService.putSpecimen(specimen, nhmdWithId);
        updateSpecimen.asset_specimen = Arrays.asList(new AssetSpecimen(updateSpecimen.asset_guid,specimen.specimen_pid(),"pinning",false));
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

    @Test
    void findSpecimenByAssetGuids() {
        roleService.addRole("NHMD");
        User nhmd = new User("findSpecimenByAssetGuids-user", Set.of("WRITE_NHMD"));
        User nhmdWithId = userService.ensureExists(nhmd);

        Asset asset_1 = AssetServiceTest.getTestAsset("findSpecimenByAssetGuids_1");
        Specimen specimen_1 = new Specimen(asset_1.institution
                , asset_1.collection
                , "findSpecimenByAssetGuids-1"
                , "nhmd.plantz.findSpecimenByAssetGuids-1"
                , new HashSet<>(Set.of("pinning", "slide"))
                ,null
                , collectionService.findCollectionInternal(asset_1.collection
                , asset_1.institution).get().collection_id()
                , Arrays.asList(new Role("NHMD"), new Role("NHMA")));
        Specimen specimen_2 = new Specimen(asset_1.institution
                , asset_1.collection
                , "findSpecimenByAssetGuids-2"
                , "nhmd.plantz.findSpecimenByAssetGuids-2"
                , new HashSet<>(Set.of("pinning"))
                ,null
                , collectionService.findCollectionInternal(asset_1.collection
                , asset_1.institution).get().collection_id()
                , new ArrayList<>());
        Specimen specimen_3 = new Specimen(asset_1.institution
                , asset_1.collection
                , "findSpecimenByAssetGuids-3"
                , "nhmd.plantz.findSpecimenByAssetGuids-3"
                , new HashSet<>(Set.of("pinning"))
                ,null
                , collectionService.findCollectionInternal(asset_1.collection
                , asset_1.institution).get().collection_id()
                , Arrays.asList(new Role("NHMD")));

        specimenService.putSpecimen(specimen_1, nhmdWithId);
        specimenService.putSpecimen(specimen_2, nhmdWithId);
        specimenService.putSpecimen(specimen_3, nhmdWithId);
        asset_1.asset_specimen = Arrays.asList(new AssetSpecimen(asset_1.asset_guid,specimen_1.specimen_pid(),"slide",false),
                new AssetSpecimen(asset_1.asset_guid,specimen_2.specimen_pid(),"pinning",false));
        assetService.persistAsset(asset_1, nhmdWithId, 86);
        Asset asset2 = AssetServiceTest.getTestAsset("findSpecimenByAssetGuids_2");
        asset2.asset_specimen = Arrays.asList(new AssetSpecimen(asset_1.asset_guid,specimen_3.specimen_pid(),"pinning",false));
        assetService.persistAsset(asset2, nhmdWithId, 86);
        Asset asset3 = AssetServiceTest.getTestAsset("findSpecimenByAssetGuids_3");
        asset3.asset_specimen = Arrays.asList(new AssetSpecimen(asset_1.asset_guid,specimen_3.specimen_pid(),"pinning",false));
        assetService.persistAsset(asset3, nhmdWithId, 86);
        Map<String, List<AssetSpecimen>> result = specimenService.getMultiAssetSpecimens(Set.of("findSpecimenByAssetGuids_1", "findSpecimenByAssetGuids_2", "findSpecimenByAssetGuids_3"));

        assertThat(result.size()).isEqualTo(3);
        assertThat(result.get("findSpecimenByAssetGuids_1").size()).isEqualTo(2);
        assertThat(result.get("findSpecimenByAssetGuids_2").size()).isEqualTo(1);
        List<AssetSpecimen> findSpecimenByAssetGuids1 = result.get("findSpecimenByAssetGuids_1");
        Optional<AssetSpecimen> first = findSpecimenByAssetGuids1.stream().filter(specimen -> specimen.specimen_pid.equals("nhmd.plantz.findSpecimenByAssetGuids-1")).findFirst();
        assertThat(first.isPresent()).isTrue();
        AssetSpecimen assetSpecimen = first.get();
        assertThat(assetSpecimen.specimen.role_restrictions()).hasSize(2);
        System.out.println(assetSpecimen.specimen.role_restrictions());
        assertThat(assetSpecimen.specimen.role_restrictions()).contains(new Role("NHMA"));
        assertThat(assetSpecimen.specimen.role_restrictions()).contains(new Role("NHMD"));

        assertThat(result.get("findSpecimenByAssetGuids_2").size()).isEqualTo(1);
        assertThat(result.get("findSpecimenByAssetGuids_2").getFirst().specimen.role_restrictions()).contains(new Role("NHMD"));

        Optional<AssetSpecimen> assetSpecimen2opt = findSpecimenByAssetGuids1.stream().filter(specimen -> specimen.specimen_pid.equals("nhmd.plantz.findSpecimenByAssetGuids-2")).findFirst();
        assertThat(assetSpecimen2opt.isPresent()).isTrue();
        AssetSpecimen assetSpecimen2 = assetSpecimen2opt.get();
        assertThat(assetSpecimen2.specimen.role_restrictions()).isEmpty();


    }

//
//    @Test
//    void listPreparationTypes() {
//    }
//



}