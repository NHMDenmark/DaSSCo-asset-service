package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.Workstation;
import dk.northtech.dasscoassetservice.domain.WorkstationStatus;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class WorkstationServiceTest extends AbstractIntegrationTest {
    @Inject
    WorkstationService workstationService;

    @Test
    void testPersistWorkstation() {
        workstationService.createWorkStation("institution_1", new Workstation("testPersistWorkstation", WorkstationStatus.IN_SERVICE, "institution_1"));
        Optional<Workstation> result = workstationService.findWorkstation("testPersistWorkstation");
        assertThat(result.isPresent()).isTrue();
        Workstation workstation = result.get();
        assertThat(workstation.name()).isEqualTo("testPersistWorkstation");
        assertThat(workstation.status()).isEqualTo(WorkstationStatus.IN_SERVICE);
    }

    @Test
    void testUpdateWorkstation() {
        Workstation workstation = new Workstation("testUpdateWorkstation", WorkstationStatus.IN_SERVICE, "institution_1");
        workstationService.createWorkStation("institution_1",workstation );
        Workstation updatedWorkstation = new Workstation(workstation.name(), WorkstationStatus.OUT_OF_SERVICE, workstation.institution_name());
        workstationService.updateWorkstation(updatedWorkstation);
        Optional<Workstation> result = workstationService.findWorkstation("testUpdateWorkstation");
        assertThat(result.isPresent()).isTrue();
        Workstation restul = result.get();
        assertThat(restul.status()).isEqualTo(WorkstationStatus.OUT_OF_SERVICE);
    }

    @Test
    void testListWorkstation() {
        List<Workstation> institution1 = workstationService.listWorkstations(new Institution("institution_1"));
        assertThat(institution1.size()).isAtLeast(2);

    }

}