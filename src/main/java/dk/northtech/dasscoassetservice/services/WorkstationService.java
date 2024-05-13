package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.Workstation;
import dk.northtech.dasscoassetservice.domain.WorkstationStatus;
import dk.northtech.dasscoassetservice.repositories.WorkstationRepository;
import jakarta.inject.Inject;
import joptsimple.internal.Strings;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WorkstationService {
    private InstitutionService institutionService;
    private WorkstationRepository workstationRepository;

    @Inject
    public WorkstationService(InstitutionService institutionService, WorkstationRepository workstationRepository) {
        this.institutionService = institutionService;
        this.workstationRepository = workstationRepository;
    }

    public List<Workstation> listWorkstations(Institution institution) {
        if (institutionService.getIfExists(institution.name()).isEmpty()){
            throw new IllegalArgumentException("Institution does not exist");
        }
        return workstationRepository.listWorkStations(institution);
    }

    public Optional<Workstation> findWorkstation(String workstationName) {
        return workstationRepository.findWorkstation(workstationName);
    }


    public Workstation createWorkStation(String institutionName, Workstation workstation) {
        if (workstation.status() == null) {
            workstation = new Workstation(workstation.name(), WorkstationStatus.IN_SERVICE, institutionName);
        }
        if (Strings.isNullOrEmpty(workstation.name())) {
            throw new RuntimeException("Workstation must have a name");
        }
        Optional<Institution> instopt = institutionService.getIfExists(institutionName);
        if (instopt.isEmpty()) {
            throw new IllegalArgumentException("Institution does not exist");
        }
        Optional<Workstation> workstationOpt = workstationRepository.findWorkstation(workstation.name());
        if (workstationOpt.isPresent()) {
            throw new IllegalArgumentException("Workstation with name [" + workstation.name() + "] already exists in institution [" + workstationOpt.get().institution_name() + "]");
        }
        Workstation newWs = new Workstation(workstation.name(), workstation.status(), institutionName);
        workstationRepository.persistWorkstation(newWs);
        return workstation;
    }

    public void updateWorkstation(Workstation workstation) {
        if (institutionService.getIfExists(workstation.institution_name()).isEmpty()){
            throw new IllegalArgumentException("Institution does not exist");
        }
        workstationRepository.updateWorkstation(workstation);
    }
}
