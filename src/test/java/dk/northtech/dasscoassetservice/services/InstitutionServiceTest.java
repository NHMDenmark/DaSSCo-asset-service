package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Institution;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Testcontainers
class InstitutionServiceTest extends AbstractIntegrationTest {
    //jdbc:postgresql://localhost:5433/dassco_asset_service?currentSchema=dassco


    @Test
    void testCreateInstitutionIllegalName() {
        InstitutionService institutionService = new InstitutionService(null);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> institutionService.createInstitution(new Institution("name'")));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Name must be alphanumeric");
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
        institutionService.createInstitution(new Institution("Teztitution"));
        //Verify that institution is not created if already exists
        List<Institution> resultList = institutionService.listInstitutions();
        assertThat(resultList.size()).isEqualTo(institutions.size());

    }

    @Test
    void testCreateInstitutionNull() {
        InstitutionService institutionService = new InstitutionService(null);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> institutionService.createInstitution(new Institution("")));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Name is cannot be null or empty");

        IllegalArgumentException illegalArgumentException2 = assertThrows(IllegalArgumentException.class, () -> institutionService.createInstitution(new Institution(null)));
        assertThat(illegalArgumentException2).hasMessageThat().isEqualTo("Name is cannot be null or empty");
    }

}