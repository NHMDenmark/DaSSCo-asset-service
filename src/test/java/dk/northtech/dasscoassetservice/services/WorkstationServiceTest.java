package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.Workstation;
import dk.northtech.dasscoassetservice.domain.WorkstationStatus;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void testPersistWorkstationAlreadyExists(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> workstationService.createWorkStation("institution_1", new Workstation("i1_w1", WorkstationStatus.IN_SERVICE, "institution_1")));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Workstation with name [i1_w1] already exists in institution [institution_1]");
    }

    @Test
    void testPersistWorkstationNoInstitution() {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> workstationService.createWorkStation("non-existent-institution", new Workstation("workstation-1", WorkstationStatus.IN_SERVICE, "non-existent-institution")));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Institution does not exist");
    }

    @Test
    void testPersistWorkstationNoName(){
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> workstationService.createWorkStation("institution_1", new Workstation("", WorkstationStatus.IN_SERVICE, "institution_1")));
        assertThat(runtimeException).hasMessageThat().isEqualTo("Workstation must have a name");
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
    void testUpdateWorkstationNonExistentInstitution(){
        Workstation workstation = new Workstation("testUpdateWorkstationFail", WorkstationStatus.IN_SERVICE, "non-existent-institution");
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> workstationService.updateWorkstation(workstation));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Institution does not exist");
    }

    @Test
    void testListWorkstation() {
        List<Workstation> institution1 = workstationService.listWorkstations(new Institution("institution_1"));
        assertThat(institution1.size()).isAtLeast(2);
    }

    @Test
    void testListWorkstationFail(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> workstationService.listWorkstations(new Institution("non-existent-institution")));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Institution does not exist");
    }

    @Test
    void testFindWorkstation(){
        Optional<Workstation> optWorkstation = workstationService.findWorkstation("i1_w1");
        assertThat(optWorkstation.isPresent()).isTrue();
        Workstation found = optWorkstation.get();
        assertThat(found.name()).isEqualTo("i1_w1");
        assertThat(found.institution_name()).isEqualTo("institution_1");
        assertThat(found.status()).isEqualTo(WorkstationStatus.IN_SERVICE);
    }

    @Test
    void testFindWorkstationFail(){
        Optional<Workstation> optionalWorkstation = workstationService.findWorkstation("non-existent-workstation");
        assertThat(optionalWorkstation.isPresent()).isFalse();
    }
}