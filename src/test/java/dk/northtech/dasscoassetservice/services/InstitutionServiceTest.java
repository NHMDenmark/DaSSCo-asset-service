package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Institution;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class InstitutionServiceTest {
    @Test
    void testCreateInstitution() {
        InstitutionService institutionService = new InstitutionService(null);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> institutionService.createInstitution(new Institution("name'")));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Name must be alphanumeric");
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