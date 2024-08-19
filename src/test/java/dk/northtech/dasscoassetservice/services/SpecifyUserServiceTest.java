package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.google.common.truth.Truth.assertThat;

class SpecifyUserServiceTest extends AbstractIntegrationTest {

    @Test
    public void newSpecifyUser() {
        SpecifyUser specifyUser = new SpecifyUser("institution_3_user", "https://this-is-an-url.net", "institution_3");

        Optional<SpecifyUser> createdUser = specifyUserService.newSpecifyUser(specifyUser);
        assertThat(createdUser.isPresent()).isTrue();
        assertThat(createdUser.get().username()).isEqualTo(specifyUser.username());
        assertThat(createdUser.get().url()).isEqualTo(specifyUser.url());
        assertThat(createdUser.get().institution()).isEqualTo(specifyUser.institution());

        SpecifyUser newSpecifyUserSameInstitution = new SpecifyUser("institution_3_user_2", "https://this-is-an-url.net", "institution_3");
        Optional<SpecifyUser> newCreatedUser = specifyUserService.newSpecifyUser(newSpecifyUserSameInstitution);
        assertThat(newCreatedUser.isEmpty()).isTrue();

        Optional<SpecifyUser> institutionUser = specifyUserService.getUserFromInstitution(createdUser.get().institution());
        assertThat(institutionUser.isPresent()).isTrue();
    }

    @Test
    public void updateSpecifyUser() {
        SpecifyUser specifyUser = new SpecifyUser("institution_4_user", "https://this-is-an-url.net", "institution_4");
        Optional<SpecifyUser> createdUser = specifyUserService.newSpecifyUser(specifyUser);
        assertThat(createdUser.isPresent()).isTrue();

        SpecifyUser newUser = new SpecifyUser("institution_4_user_updated", createdUser.get().url(), createdUser.get().institution());

        Optional<SpecifyUser> userByUsername = specifyUserService.getUserFromUsername(createdUser.get().username());
        System.out.println(userByUsername);

        Optional<SpecifyUser> updatedUser = specifyUserService.updateUser(createdUser.get().institution(), newUser);
        System.out.println(updatedUser);
        assertThat(updatedUser.isPresent()).isTrue();
        assertThat(updatedUser.get().username()).isEqualTo(newUser.username());
    }

    @Test
    public void getAndDeleteSpecifyUser() {
        SpecifyUser specifyUser = new SpecifyUser("institution_5_user", "https://this-is-an-url.net", "institution_5");
        Optional<SpecifyUser> createdUser = specifyUserService.newSpecifyUser(specifyUser);
        assertThat(createdUser.isPresent()).isTrue();

        Optional<SpecifyUser> userByUsername = specifyUserService.getUserFromUsername(specifyUser.username());
        assertThat(userByUsername.isPresent()).isTrue();

        Optional<SpecifyUser> deletedUser = specifyUserService.deleteUser(specifyUser.institution());
        assertThat(deletedUser.isPresent()).isTrue();

        Optional<SpecifyUser> userByUsernameDeleted = specifyUserService.getUserFromUsername(specifyUser.username());
        assertThat(userByUsernameDeleted.isPresent()).isFalse();

        Optional<SpecifyUser> userByInstitutionDeleted = specifyUserService.getUserFromInstitution(specifyUser.institution());
        assertThat(userByInstitutionDeleted.isPresent()).isFalse();
    }
}