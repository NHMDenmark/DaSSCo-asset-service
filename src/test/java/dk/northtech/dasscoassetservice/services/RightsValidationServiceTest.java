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
    public void testCheckRights() {
        Institution institution = new Institution("inst_rv1", Arrays.asList(new Role("test_1")));
        institutionService.createInstitution(institution);
        Collection collection = new Collection("col_rv1", institution.name(),new ArrayList<>());
        collectionService.persistCollection(collection);

        boolean result = rightsValidationService.checkRights(getUser("WRITE_test_1"), institution.name(), collection.name(), true);
        assertThat(result).isTrue();
    }

    public User getUser(String... roles) {
        User user = new User();
        user.roles = new HashSet<>(Arrays.asList(roles));
        return user;
    }

}