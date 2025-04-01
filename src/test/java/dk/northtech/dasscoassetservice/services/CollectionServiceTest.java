package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.Role;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.checkerframework.checker.lock.qual.Holding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class CollectionServiceTest extends AbstractIntegrationTest {

    @Inject
    private CollectionService collectionService;
    @Test
    void testPersistCollection() {
        collectionService.persistCollection(new Collection("Testology", "institution_1", new ArrayList<>()));
        Optional<Collection> testology = collectionService.findCollectionInternal("Testology","institution_1");
        assertThat(testology.isPresent()).isTrue();
        Collection resutl = testology.get();
        // Verify that it is actually saved
        assertThat(resutl.collection_id()).isNotNull();
        assertThat(resutl.collection_id()).isGreaterThan(0);
        assertThat(resutl.name()).isEqualTo("Testology");
    }

    @Test
    void testSameNameDiffInstitutions() {
        institutionService.createInstitution(new Institution("testSameNameDiffInstitutions_1", new ArrayList<>()));
        institutionService.createInstitution(new Institution("testSameNameDiffInstitutions_2", new ArrayList<>()));
        collectionService.persistCollection(new Collection("c_testSameNameDiffInstitutions", "testSameNameDiffInstitutions_1", Arrays.asList(new Role("role_1"))));
        collectionService.persistCollection(new Collection("c_testSameNameDiffInstitutions", "testSameNameDiffInstitutions_2", Arrays.asList(new Role("role_2"))));
        Optional<Collection> resultOpt = collectionService.findCollectionInternal("c_testSameNameDiffInstitutions","testSameNameDiffInstitutions_1");
        Collection collection = resultOpt.get();
        assertThat(collection.roleRestrictions()).hasSize(1);
        assertThat(collection.roleRestrictions().get(0).name()).isEqualTo("role_1");
        Optional<Collection> resultOpt2 = collectionService.findCollectionInternal("c_testSameNameDiffInstitutions","testSameNameDiffInstitutions_2");
        Collection collection2 = resultOpt2.get();
        assertThat(collection2.roleRestrictions()).hasSize(1);
        assertThat(collection2.roleRestrictions().get(0).name()).isEqualTo("role_2");
    }

    @Test
    void testPersistCollectionWithRoles() {
        collectionService.persistCollection(new Collection("test-collection-roles", "institution_1", Arrays.asList(new Role("test-role"))));
        collectionService.persistCollection(new Collection("test-collection-no-roles", "institution_1",new ArrayList<>()));
        Optional<Collection> result1opt = collectionService.findCollectionInternal("test-collection-roles","institution_1");
        assertThat(result1opt.isPresent()).isTrue();
        Collection resutl = result1opt.get();
        assertThat(resutl.name()).isEqualTo("test-collection-roles");
        assertThat(resutl.roleRestrictions()).hasSize(1);
        Collection collection = new Collection(resutl.name(), resutl.institution(), Arrays.asList(new Role("test-role-1"), new Role("test-role-2")));
        collectionService.updateCollection(collection);
        Optional<Collection> result2opt = collectionService.findCollectionInternal("test-collection-roles","institution_1");
        Collection result2 = result2opt.orElseThrow(()->new RuntimeException("Failed"));
        assertThat(result2.roleRestrictions()).hasSize(2);

        //Verify that roles are only added to targeted Collection
        Optional<Collection> result3opt = collectionService.findCollectionInternal("test-collection-no-roles","institution_1");
        Collection result3 = result3opt.orElseThrow(()->new RuntimeException("Failed"));
        assertThat(result3.roleRestrictions()).hasSize(0);


    }

    @Test
    void testPersistCollectionAlreadyExists() {
        collectionService.persistCollection(new Collection("persistCollectionAlreadyExists", "institution_1", new ArrayList<>()));
        Institution institution1 = new Institution("institution_1");
        List<Collection> before = collectionService.listCollections(institution1,user);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> collectionService.persistCollection(new Collection("persistCollectionAlreadyExists", "institution_1", new ArrayList<>())));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Collection already exists in this institution");
        List<Collection> after = collectionService.listCollections(institution1,user);
        assertThat(before.size()).isEqualTo(after.size());
    }

    @Test
    void testPersistCollectionInstitutionDoesntExist() {
        Collection collection = new Collection("persistCollectionAlreadyExists", "DOESNT_EXIST", new ArrayList<>());
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> collectionService.persistCollection(collection));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Institute doesnt exist");
    }

    @Test
    void testPersistCollectionNameIsNull(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> collectionService.persistCollection(new Collection("", "institution_1", new ArrayList<>())));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Name cannot be null or empty");
    }

    @Test
    void testPersistCollectionCorrectInstitutionNoCollection(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> collectionService.persistCollection(null));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("POST method requires a body");
    }

    @Test
    void testListCollections(){
        List<Collection> collections = collectionService.listCollections(new Institution("institution_1"),user);
        assertThat(collections.size()).isAtLeast(2);
    }

    @Test
    void testListCollectionInstitutionDoesNotExist(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> collectionService.listCollections(new Institution("non-existent-institution"),user));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Institution does not exist");
    }

    @Test
    void testFindCollection(){
        Optional<Collection> optCollection = collectionService.findCollectionInternal("i1_c1","institution_1");
        assertThat(optCollection.isPresent()).isTrue();
        Collection exists = optCollection.get();
        assertThat(exists.name()).isEqualTo("i1_c1");
    }

    @Test
    void testReadAll(){
        List<Collection> all = collectionService.getAll();
        assertThat(all.size()).isGreaterThan(0);
    }

    @Test
    void testFindCollectionDoesntExist(){
        Optional<Collection> optCollection = collectionService.findCollectionInternal("does-not-exist","institution_1");
        assertThat(optCollection.isPresent()).isFalse();
    }

    @Test
    void dummy() {
        System.out.println("hell o");
    }
}