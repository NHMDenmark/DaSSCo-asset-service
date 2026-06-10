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
        specimen = specimenService.putSpecimen(specimen, nhmdWithId);
        AssetSpecimen updateSpecimenLink = new AssetSpecimen(updateSpecimen.asset_guid,specimen.specimen_pid(),"pinning",false);
        updateSpecimenLink.specimen_id = specimen.specimen_id();
        updateSpecimenLink.specimen = specimen;
        updateSpecimen.asset_specimen = Arrays.asList(updateSpecimenLink);
        assetService.persistAsset(updateSpecimen, nhmdWithId, 86);
        Optional<Specimen> resultSpecimen = specimenService.findSpecimen(updateSpecimen.institution, updateSpecimen.collection, "updateSpecimenDoNotRemovePrepTypeInUse-1");
        assertThat(resultSpecimen.isPresent()).isTrue();
        Specimen specimen1 = resultSpecimen.get();
        assertThat(specimen1.role_restrictions()).contains(new Role("NHMD"));

        Specimen illegalPrepTypeRemoval = new Specimen(updateSpecimen.institution
                , updateSpecimen.collection
                , "updateSpecimenDoNotRemovePrepTypeInUse-1"
                , "nhmd.plantz.updateSpecimenDoNotRemovePrepTypeInUse-1"
                , new HashSet<>(Set.of("slide"))
                ,null, collectionService.findCollectionInternal(updateSpecimen.collection
                , updateSpecimen.institution).get().collection_id()
                ,Arrays.asList(new Role("NHMD")));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> {
            specimenService.putSpecimen(illegalPrepTypeRemoval, nhmd);
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

        specimen_1 = specimenService.putSpecimen(specimen_1, nhmdWithId);
        specimen_2 = specimenService.putSpecimen(specimen_2, nhmdWithId);
        specimen_3 = specimenService.putSpecimen(specimen_3, nhmdWithId);
        AssetSpecimen asset1Specimen1 = new AssetSpecimen(asset_1.asset_guid,specimen_1.specimen_pid(),"slide",false);
        asset1Specimen1.specimen_id = specimen_1.specimen_id();
        asset1Specimen1.specimen = specimen_1;
        AssetSpecimen asset1Specimen2 = new AssetSpecimen(asset_1.asset_guid,specimen_2.specimen_pid(),"pinning",false);
        asset1Specimen2.specimen_id = specimen_2.specimen_id();
        asset1Specimen2.specimen = specimen_2;
        asset_1.asset_specimen = Arrays.asList(asset1Specimen1, asset1Specimen2);
        assetService.persistAsset(asset_1, nhmdWithId, 86);
        Asset asset2 = AssetServiceTest.getTestAsset("findSpecimenByAssetGuids_2");
        AssetSpecimen asset2Specimen = new AssetSpecimen(asset_1.asset_guid,specimen_3.specimen_pid(),"pinning",false);
        asset2Specimen.specimen_id = specimen_3.specimen_id();
        asset2Specimen.specimen = specimen_3;
        asset2.asset_specimen = Arrays.asList(asset2Specimen);
        assetService.persistAsset(asset2, nhmdWithId, 86);
        Asset asset3 = AssetServiceTest.getTestAsset("findSpecimenByAssetGuids_3");
        AssetSpecimen asset3Specimen = new AssetSpecimen(asset_1.asset_guid,specimen_3.specimen_pid(),"pinning",false);
        asset3Specimen.specimen_id = specimen_3.specimen_id();
        asset3Specimen.specimen = specimen_3;
        asset3.asset_specimen = Arrays.asList(asset3Specimen);
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

    @Test
    void updateSpecimenRemovesRoleRestrictionsMissingFromRequest() {
        Asset asset = AssetServiceTest.getTestAsset("updateSpecimenRemovesRoleRestrictionsMissingFromRequest");
        User user = new User("update-specimen-roles-user", Set.of("WRITE_updateSpecimenRemoveRoleKeep", "WRITE_updateSpecimenRemoveRoleDelete"));

        Specimen specimen = new Specimen(asset.institution
                , asset.collection
                , "updateSpecimenRemovesRoleRestrictionsMissingFromRequest-1"
                , "nhmd.plantz.updateSpecimenRemovesRoleRestrictionsMissingFromRequest-1"
                , new HashSet<>(Set.of("pinning", "slide"))
                , null
                , collectionService.findCollectionInternal(asset.collection, asset.institution).get().collection_id()
                , Arrays.asList(new Role("updateSpecimenRemoveRoleKeep"), new Role("updateSpecimenRemoveRoleDelete")));
        Specimen persisted = specimenService.putSpecimen(specimen, user);

        Specimen update = new Specimen(asset.institution
                , asset.collection
                , "updateSpecimenRemovesRoleRestrictionsMissingFromRequest-1"
                , "nhmd.plantz.updateSpecimenRemovesRoleRestrictionsMissingFromRequest-1"
                , new HashSet<>(Set.of("pinning", "slide"))
                , persisted.specimen_id()
                , persisted.collection_id()
                , List.of(new Role("updateSpecimenRemoveRoleKeep")));

        specimenService.putSpecimen(update, user);

        List<String> persistedRoles = jdbi.withHandle(handle -> handle.createQuery("""
                        SELECT role
                        FROM specimen_role_restriction
                        WHERE specimen_id = :specimenId
                        """)
                .bind("specimenId", persisted.specimen_id())
                .mapTo(String.class)
                .list());
        assertThat(persistedRoles).containsExactly("updateSpecimenRemoveRoleKeep");
    }

    @Test
    void findSpecimenRequiresReadAccessToRoleRestrictions() {
        Asset asset = AssetServiceTest.getTestAsset("findSpecimenRequiresReadAccessToRoleRestrictions");
        User writer = userService.ensureExists(new User("find-specimen-role-writer", Set.of("WRITE_findSpecimenRestrictedRole")));
        User reader = userService.ensureExists(new User("find-specimen-role-reader", Set.of("READ_findSpecimenRestrictedRole")));
        User noAccessUser = userService.ensureExists(new User("find-specimen-role-no-access", Set.of()));

        Specimen specimen = new Specimen(asset.institution,
                asset.collection,
                "findSpecimenRequiresReadAccessToRoleRestrictions-1",
                "nhmd.plantz.findSpecimenRequiresReadAccessToRoleRestrictions-1",
                new HashSet<>(Set.of("pinning")),
                null,
                collectionService.findCollectionInternal(asset.collection, asset.institution).get().collection_id(),
                List.of(new Role("findSpecimenRestrictedRole")));

        specimenService.putSpecimen(specimen, writer);

        assertThrows(DasscoIllegalActionException.class, () ->
                specimenService.findSpecimen(asset.institution, asset.collection, "findSpecimenRequiresReadAccessToRoleRestrictions-1", noAccessUser));

        Optional<Specimen> foundByReader = specimenService.findSpecimen(asset.institution, asset.collection, "findSpecimenRequiresReadAccessToRoleRestrictions-1", reader);
        assertThat(foundByReader.isPresent()).isTrue();
        assertThat(foundByReader.get().role_restrictions()).containsExactly(new Role("findSpecimenRestrictedRole"));

        Optional<Specimen> foundByWriter = specimenService.findSpecimen(asset.institution, asset.collection, "findSpecimenRequiresReadAccessToRoleRestrictions-1", writer);
        assertThat(foundByWriter.isPresent()).isTrue();
        assertThat(foundByWriter.get().role_restrictions()).containsExactly(new Role("findSpecimenRestrictedRole"));
    }

    @Test
    void updateSpecimenRequiresWriteAccessToRoleRestrictions() {
        Asset asset = AssetServiceTest.getTestAsset("updateSpecimenRequiresWriteAccessToRoleRestrictions");
        User writer = userService.ensureExists(new User("update-specimen-role-writer", Set.of("WRITE_updateSpecimenRestrictedRole")));
        User noAccessUser = userService.ensureExists(new User("update-specimen-role-no-access", Set.of()));

        Specimen specimen = new Specimen(asset.institution,
                asset.collection,
                "updateSpecimenRequiresWriteAccessToRoleRestrictions-1",
                "nhmd.plantz.updateSpecimenRequiresWriteAccessToRoleRestrictions-1",
                new HashSet<>(Set.of("pinning")),
                null,
                collectionService.findCollectionInternal(asset.collection, asset.institution).get().collection_id(),
                List.of(new Role("updateSpecimenRestrictedRole")));

        Specimen persisted = specimenService.putSpecimen(specimen, writer);

        Specimen update = new Specimen(asset.institution,
                asset.collection,
                "updateSpecimenRequiresWriteAccessToRoleRestrictions-1",
                "nhmd.plantz.updateSpecimenRequiresWriteAccessToRoleRestrictions-1",
                new HashSet<>(Set.of("pinning", "slide")),
                persisted.specimen_id(),
                persisted.collection_id(),
                List.of(new Role("updateSpecimenRestrictedRole")));

        assertThrows(DasscoIllegalActionException.class, () -> specimenService.putSpecimen(update, noAccessUser));

        Specimen updated = specimenService.putSpecimen(update, writer);
        assertThat(updated.preparation_types()).containsExactly("pinning", "slide");
        assertThat(updated.role_restrictions()).containsExactly(new Role("updateSpecimenRestrictedRole"));
    }

//
//    @Test
//    void listPreparationTypes() {
//    }
//



}
