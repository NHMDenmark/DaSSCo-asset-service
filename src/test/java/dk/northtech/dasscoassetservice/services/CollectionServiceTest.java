package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Institution;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CollectionServiceTest extends AbstractIntegrationTest {

    @Inject
    private CollectionService collectionService;
    @Test
    void persistCollection() {
        collectionService.persistCollection(new Collection("Testology", "institution_1"));
        Optional<Collection> testology = collectionService.findCollection("Testology");
        assertThat(testology.isPresent()).isTrue();
        Collection resutl = testology.get();
        assertThat(resutl.name()).isEqualTo("Testology");
    }

    @Test
    void persistCollectionAlreadyExists() {
        collectionService.persistCollection(new Collection("persistCollectionAlreadyExists", "institution_1"));
        Institution institution1 = new Institution("institution_1");
        List<Collection> before = collectionService.listCollections(institution1);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> collectionService.persistCollection(new Collection("persistCollectionAlreadyExists", "institution_1")));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Collection already exists in this institute");
        List<Collection> after = collectionService.listCollections(institution1);
        assertThat(before.size()).isEqualTo(after.size());
    }

    @Test
    void persistCollectionInstitutionDoesntExist() {
        Collection collection = new Collection("persistCollectionAlreadyExists", "DOESNT_EXIST");
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> collectionService.persistCollection(collection));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Institute doesnt exist");
    }
}