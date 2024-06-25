package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.Role;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CollectionServiceTest extends AbstractIntegrationTest {

    @Inject
    private CollectionService collectionService;
    @Test
    void testPersistCollection() {
        collectionService.persistCollection(new Collection("Testology", "institution_1", new ArrayList<>()), "institution_1");
        Optional<Collection> testology = collectionService.findCollection("Testology");
        assertThat(testology.isPresent()).isTrue();
        Collection resutl = testology.get();
        assertThat(resutl.name()).isEqualTo("Testology");
    }

    @Test
    void testPersistCollectionWithRoles() {
        collectionService.persistCollection(new Collection("test-collection-roles", "institution_1", Arrays.asList(new Role("test-role"))), "institution_1");
        Optional<Collection> testology = collectionService.findCollection("test-collection-roles");
        assertThat(testology.isPresent()).isTrue();
        Collection resutl = testology.get();
        assertThat(resutl.name()).isEqualTo("test-collection-roles");
        assertThat(resutl.roleRestrictions()).hasSize(1);
    }

    @Test
    void testPersistCollectionAlreadyExists() {
        collectionService.persistCollection(new Collection("persistCollectionAlreadyExists", "institution_1", new ArrayList<>()), "institution_1");
        Institution institution1 = new Institution("institution_1");
        List<Collection> before = collectionService.listCollections(institution1);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> collectionService.persistCollection(new Collection("persistCollectionAlreadyExists", "institution_1", new ArrayList<>()), "institution_1"));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Collection already exists in this institute");
        List<Collection> after = collectionService.listCollections(institution1);
        assertThat(before.size()).isEqualTo(after.size());
    }

    @Test
    void testPersistCollectionInstitutionDoesntExist() {
        Collection collection = new Collection("persistCollectionAlreadyExists", "DOESNT_EXIST", new ArrayList<>());
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> collectionService.persistCollection(collection, "DOESNT_EXIST"));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Institute doesnt exist");
    }

    @Test
    void testPersistCollectionNameIsNull(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> collectionService.persistCollection(new Collection("", "institution_1", new ArrayList<>()), "institution_1"));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Name cannot be null or empty");
    }

    @Test
    void testPersistCollectionCorrectInstitutionNoCollection(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> collectionService.persistCollection(null, "institution_1"));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("POST method requires a body");
    }

    @Test
    void testListCollections(){
        List<Collection> collections = collectionService.listCollections(new Institution("institution_1"));
        assertThat(collections.size()).isAtLeast(2);
    }

    @Test
    void testListCollectionInstitutionDoesNotExist(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> collectionService.listCollections(new Institution("non-existent-institution")));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Institute doesnt exist");
    }

    @Test
    void testFindCollection(){
        Optional<Collection> optCollection = collectionService.findCollection("i1_c1");
        assertThat(optCollection.isPresent()).isTrue();
        Collection exists = optCollection.get();
        assertThat(exists.name()).isEqualTo("i1_c1");
    }

    @Test
    void testFindCollectionDoesntExist(){
        Optional<Collection> optCollection = collectionService.findCollection("does-not-exist");
        assertThat(optCollection.isPresent()).isFalse();
    }
}