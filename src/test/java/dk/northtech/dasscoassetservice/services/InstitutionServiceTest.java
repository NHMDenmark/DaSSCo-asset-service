package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Institution;
import org.junit.jupiter.api.Test;

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
        InstitutionService institutionService = new InstitutionService(null);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> institutionService.createInstitution(new Institution("")));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Name cannot be null or empty");
    }

    @Test
    void testCreateInstitution() {
        institutionService.createInstitution(new Institution("Teztitution"));
        List<Institution> institutions = institutionService.listInstitutions();
        System.out.println(institutions.size());
        Optional<Institution> result = institutions.stream().filter(institution -> {
            return institution.name().equals("Teztitution");
        }).findAny();
        assertThat(result.isPresent()).isTrue();
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