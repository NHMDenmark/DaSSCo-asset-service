package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.Role;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


//@SpringBootTest
//@Testcontainers
class InstitutionServiceTest extends AbstractIntegrationTest {
    //jdbc:postgresql://localhost:5433/dassco_asset_service?currentSchema=dassco


    @Test
    void testCreateInstitutionIllegalName() {
        InstitutionService institutionService = new InstitutionService(null,null, null,null);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> institutionService.createInstitution(new Institution("")));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Name cannot be null or empty");
    }

    @Test
    void testCreateInstitutionNoBody(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> institutionService.createInstitution(null));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("POST request requires a body");
    }

    @Test
    void testCreateInstitution() {
        institutionService.createInstitution(new Institution("Teztitution"));
        List<Institution> institutions = institutionService.listInstitutions();
        Optional<Institution> result = institutions.stream().filter(institution -> {
            return institution.name().equals("Teztitution");
        }).findAny();
        assertThat(result.isPresent()).isTrue();
    }

    @Test
    void testUpdateInstitution() {
        institutionService.createInstitution(new Institution("ikz"));
        List<Institution> institutions = institutionService.listInstitutions();
        Optional<Institution> createdOpt = institutions.stream().filter(institution -> {
            return institution.name().equals("ikz");
        }).findAny();
        assertThat(createdOpt.isPresent()).isTrue();
        Institution institution = createdOpt.get();
        institution.roleRestrictions().add(new Role("ikz"));
        institutionService.updateInstitution(institution);
        institutions = institutionService.listInstitutions();
        Optional<Institution> updatedOpt = institutions.stream().filter(i -> {
            return i.name().equals("ikz");
        }).findAny();
        assertThat(updatedOpt.isPresent()).isTrue();
        Institution result = updatedOpt.get();
        assertThat(result.name()).isEqualTo("ikz");
        assertThat(result.roleRestrictions().size()).isEqualTo(1);
        assertThat(result.roleRestrictions().get(0).name()).isEqualTo("ikz");

    }

    @Test
    void testInstitutionRoles() {
        institutionService.createInstitution(new Institution("teztitution_rolez", Arrays.asList(new Role("super-burger"), new Role("super-duper-bruger"))));
        // Verify that institutions can be initialized correct
        institutionService.initInstitutions(true);
        List<Institution> institutions = institutionService.listInstitutions();
        Optional<Institution> result = institutions.stream().filter(institution -> {
            return institution.name().equals("teztitution_rolez");
        }).findAny();
        assertThat(result.isPresent()).isTrue();
        Institution institution = result.get();
        assertThat(institution.roleRestrictions()).hasSize(2);
        Institution institution1 = new Institution(institution.name(), new ArrayList<>());
        institutionService.updateInstitution(institution1);
        Optional<Institution> resultOpt = institutionService.getIfExists("teztitution_rolez");
        Institution resultLast = resultOpt.orElseThrow(RuntimeException::new);
        assertThat(resultLast.roleRestrictions()).isEmpty();
    }

    @Test
    void testCreateInstitutionAlreadyExists(){
        List<Institution> institutions = institutionService.listInstitutions();
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> institutionService.createInstitution(new Institution("institution_1")));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Institute already exists");
        //Verify that institution is not created if already exists
        List<Institution> resultList = institutionService.listInstitutions();
        assertThat(resultList.size()).isEqualTo(institutions.size());
    }

    @Test
    void testListInstitutions(){
        List<Institution> institutions = institutionService.listInstitutions();
        assertThat(institutions.size()).isAtLeast(2);
    }

    @Test
    void testGetIfExistst(){
        Optional<Institution> optInstitution = institutionService.getIfExists("institution_1");
        assertThat(optInstitution.isPresent()).isTrue();
        Institution exists = optInstitution.get();
        assertThat(exists.name()).isEqualTo("institution_1");
    }

    @Test
    void testGetIfExistsDoesNotExist(){
        Optional<Institution> optInstitution = institutionService.getIfExists("non-existent-institution");
        assertThat(optInstitution.isPresent()).isFalse();
    }
}