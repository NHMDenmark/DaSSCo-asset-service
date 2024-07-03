package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.Role;
import dk.northtech.dasscoassetservice.domain.User;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class RightsValidationServiceTest extends AbstractIntegrationTest {
    @Test
    public void testCheckRightsInstitution() {
        Institution institution = new Institution("inst_rv1", Arrays.asList(new Role("test_1")));
        institutionService.createInstitution(institution);
        Collection collection = new Collection("col_rv1", institution.name(), new ArrayList<>());
        collectionService.persistCollection(collection);

        boolean result = rightsValidationService.checkWriteRights(getUser("WRITE_test_1"), institution.name(), collection.name());
        assertThat(result).isTrue();
        //user does not have write right
        assertThat(rightsValidationService.checkWriteRights(getUser("READ_test_1"), institution.name(), collection.name())).isFalse();
    }

    @Test
    public void testCheckReadRightsInstitution() {
        Institution institution = new Institution("inst_testCheckReadRightsInstitution", Arrays.asList(new Role("testCheckReadRightsInstitution")));
        institutionService.createInstitution(institution);
        Collection collection = new Collection("col_testCheckReadRightsInstitution", institution.name(), new ArrayList<>());
        collectionService.persistCollection(collection);

        assertThat(rightsValidationService.checkReadRights(getUser("READ_testCheckReadRightsInstitution"), institution.name(), collection.name())).isTrue();
        //Write also grant read
        assertThat(rightsValidationService.checkReadRights(getUser("WRITE_testCheckReadRightsInstitution"), institution.name(), collection.name())).isTrue();
        assertThat(rightsValidationService.checkWriteRights(getUser("READ_not_on_inst"), institution.name(), collection.name())).isFalse();
    }

    @Test
    public void testCheckWriteRightsCollection() {
        Institution institution = new Institution("inst_testCheckWriteRightsCollection", new ArrayList<>());
        institutionService.createInstitution(institution);
        Collection collection = new Collection("col_testCheckWriteRightsCollection", institution.name(), Arrays.asList(new Role("testCheckWriteRightsCollection"), new Role("testCheckWriteRightsCollection_2")));
        collectionService.persistCollection(collection);

        assertThat(rightsValidationService.checkWriteRights(getUser("WRITE_testCheckWriteRightsCollection"), institution.name(), collection.name())).isTrue();
        //user does not have write right
        assertThat(rightsValidationService.checkWriteRights(getUser("READ_testCheckWriteRightsCollection"), institution.name(), collection.name())).isFalse();
    }

    @Test
    public void testCheckReadRightsCollection() {
        Institution institution = new Institution("inst_testCheckReadRightsCollection", new ArrayList<>());
        institutionService.createInstitution(institution);
        Collection collection = new Collection("col_testCheckReadRightsCollection", institution.name(), Arrays.asList(new Role("testCheckReadRightsCollection"), new Role("testCheckReadRightsCollection_2")));
        collectionService.persistCollection(collection);

        assertThat(rightsValidationService.checkWriteRights(getUser("READ_testCheckReadRightsCollection"), institution.name(), collection.name())).isFalse();
        //user does not have write right
        assertThat(rightsValidationService.checkWriteRights(getUser("WRITE_testCheckReadRightsCollection"), institution.name(), collection.name())).isTrue();
    }

    public User getUser(String... roles) {
        User user = new User();
        user.roles = new HashSet<>(Arrays.asList(roles));
        return user;
    }

}