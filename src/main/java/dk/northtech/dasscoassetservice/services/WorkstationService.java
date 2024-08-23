package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.cache.InstitutionCache;
import dk.northtech.dasscoassetservice.cache.WorkstationCache;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.Workstation;
import dk.northtech.dasscoassetservice.domain.WorkstationStatus;
import dk.northtech.dasscoassetservice.repositories.WorkstationRepository;
import jakarta.inject.Inject;
import joptsimple.internal.Strings;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class WorkstationService {
    private InstitutionService institutionService;
    private WorkstationRepository workstationRepository;
    private InstitutionCache institutionCache;
    private WorkstationCache workstationCache;

    @Inject
    public WorkstationService(InstitutionService institutionService, WorkstationRepository workstationRepository,
                              InstitutionCache institutionCache, WorkstationCache workstationCache) {
        this.institutionService = institutionService;
        this.workstationRepository = workstationRepository;
        this.institutionCache = institutionCache;
        this.workstationCache = workstationCache;
    }

    public List<Workstation> listWorkstations(Institution institution) {
        if (!institutionCache.institutionExists(institution.name())){
            throw new IllegalArgumentException("Institution does not exist");
        }

        return workstationCache.getWorkstations(institution.name());
    }

    public Optional<Workstation> findWorkstation(String workstationName) {
        return workstationRepository.findWorkstation(workstationName);
    }


    public Workstation createWorkStation(String institutionName, Workstation workstation) {

        if (Objects.isNull(workstation)){
            throw new IllegalArgumentException("POST request requires a body");
        }

        if (workstation.status() == null) {
            workstation = new Workstation(workstation.name(), WorkstationStatus.IN_SERVICE, institutionName);
        }

        if (Strings.isNullOrEmpty(workstation.name())) {
            throw new IllegalArgumentException("Workstation must have a name");
        }

        if (!institutionCache.institutionExists(institutionName)){
            throw new IllegalArgumentException("Institution does not exist");
        }

        Workstation exists = workstationCache.workstationExists(workstation.name());
        if (exists != null){
            throw new IllegalArgumentException("Workstation with name [" + exists.name() + "] already exists in institution [" + exists.institution_name() + "]");
        }

        Workstation newWs = new Workstation(workstation.name(), workstation.status(), institutionName);
        workstationRepository.persistWorkstation(newWs);
        workstationCache.putWorkstationInCacheIfAbsent(workstation);

        return workstation;
    }

    public void updateWorkstation(Workstation workstation, String institutionName) {

        if (Objects.isNull(workstation)){
            throw new IllegalArgumentException("UPDATE request requires a body");
        }

        if (workstation.status() == null){
            throw new IllegalArgumentException("Workstation needs a STATUS to be updated");
        }

        if (workstation.name() == null){
            throw new IllegalArgumentException("Workstation name must be present");
        }

        if (!institutionCache.institutionExists(institutionName)){
            throw new IllegalArgumentException("Institution does not exist");
        }

        Workstation newWs = new Workstation(workstation.name(), workstation.status(), institutionName);

        workstationRepository.updateWorkstation(newWs);
        // "Put" will replace the existing workstation with the new one, keeping the key.
        workstationCache.putWorkstationInCacheIfAbsent(newWs);
    }
}
