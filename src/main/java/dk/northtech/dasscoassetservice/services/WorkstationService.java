package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.Workstation;
import dk.northtech.dasscoassetservice.repositories.WorkstationRepository;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WorkstationService {
    private InstitutionService institutionService;
    private WorkstationRepository workstationRepository;

    @Inject
    public WorkstationService(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    public List<Workstation> listWorkstations(Institution institution) {

        return null;
    }

    public Optional<Workstation> findWorkstation(String workstationName) {
        return workstationRepository.findWorkstation(workstationName);
    }


    public Workstation createWorkStation(String institutionName, Workstation workstation) {
        Optional<Institution> instopt = institutionService.getIfExists(institutionName);
        if (instopt.isEmpty()) {
            throw new IllegalArgumentException("Institution does not exist");
        }
        Optional<Workstation> workstationOpt = workstationRepository.findWorkstation(workstation.name());
        if (workstationOpt.isPresent()) {
            throw new IllegalArgumentException("Workstation with name [" + workstation.name() + "] already exists in institution [" + workstationOpt.get().institution_name() + "]");
        }
        workstationRepository.persistWorkstation(workstation);
        return workstation;
    }
}
