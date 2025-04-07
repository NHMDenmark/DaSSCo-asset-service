package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.cache.InstitutionCache;
import dk.northtech.dasscoassetservice.cache.WorkstationCache;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.Pipeline;
import dk.northtech.dasscoassetservice.domain.Workstation;
import dk.northtech.dasscoassetservice.domain.WorkstationStatus;
import dk.northtech.dasscoassetservice.repositories.PipelineRepository;
import dk.northtech.dasscoassetservice.repositories.WorkstationRepository;
import jakarta.inject.Inject;
import joptsimple.internal.Strings;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WorkstationService {
    private InstitutionService institutionService;
    private static final Logger logger = LoggerFactory.getLogger(WorkstationService.class);
    private Jdbi jdbi;
    private boolean initialised = false;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Workstation>> workstationMap = new ConcurrentHashMap<>();
    @Inject
    public WorkstationService(InstitutionService institutionService, Jdbi jdbi) {
        this.institutionService = institutionService;
        this.jdbi = jdbi;
    }

    public void forceCacheRefresh() {
        this.initWorkstations(true);
    }


    public List<Workstation> listWorkstations(Institution institution) {
        if (institutionService.getIfExists(institution.name()).isEmpty()){
            throw new IllegalArgumentException("Institution does not exist");
        }
        if(!this.initialised) {
            this.initWorkstations();
        }
        if(workstationMap.containsKey(institution.name())){
            return new ArrayList<>(workstationMap.get(institution.name()).values());
        }
        return new ArrayList<>();
    }

    private void initWorkstations() {
        this.initWorkstations(false);
    }

    private void initWorkstations(boolean force) {
        synchronized (this) {
            if (!this.initialised || force) {
                jdbi.withHandle(h -> {
                    this.workstationMap.clear();
                    this.institutionService.listInstitutions().forEach(i -> this.workstationMap.put(i.name(), new ConcurrentHashMap<>()));
                    WorkstationRepository workstationRepository = h.attach(WorkstationRepository.class);
                    List<Workstation> workstations = workstationRepository.listWorkStations();
                    for (Workstation workstation: workstations) {
                        this.workstationMap.get(workstation.institution_name()).put(workstation.name(), workstation);
                    }
                    logger.info("Loaded {} collections", workstationMap.size());
                    return h;
                });
                this.initialised = true;
            }
        }
    }

//    public Optional<Workstation> findWorkstation(String workstationName) {
//        return workstationRepository.findWorkstation(workstationName);
//    }


    public Optional<Workstation> findWorkstation(String workstation_name, String institution_name) {
        if(!this.initialised) {
            this.initWorkstations();
        }
        if(Strings.isNullOrEmpty(workstation_name) || !workstationMap.containsKey(institution_name) || !workstationMap.get(institution_name).containsKey(workstation_name)) {
            return Optional.empty();
        }
        return Optional.of(workstationMap.get(institution_name).get(workstation_name));
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

        if (institutionService.getIfExists(institutionName).isEmpty()){
            throw new IllegalArgumentException("Institution does not exist");
        }

        Optional<Workstation> optExisting = findWorkstation(workstation.name(), institutionName);
        if (optExisting.isPresent()){
            Workstation exists = optExisting.get();
            throw new IllegalArgumentException("Workstation with name [" + exists.name() + "] already exists in institution [" + exists.institution_name() + "]");
        }

        Workstation newWs = new Workstation(workstation.name(), workstation.status(), institutionName);
        jdbi.withHandle(h -> {
            WorkstationRepository attach = h.attach(WorkstationRepository.class);
            Workstation persistedWorkstation = attach.persistWorkstation(newWs);
            this.workstationMap.computeIfAbsent(persistedWorkstation.institution_name(), i -> new ConcurrentHashMap<>()).put(persistedWorkstation.name(), persistedWorkstation);
            return h;
        });

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

        if (institutionService.getIfExists(institutionName).isEmpty()){
            throw new IllegalArgumentException("Institution does not exist");
        }
        Optional<Workstation> existingOpt = findWorkstation(workstation.name(), workstation.institution_name());
        if(existingOpt.isEmpty()){
            throw new IllegalArgumentException("Workstation doesnt exist");
        }
        Workstation existing = existingOpt.get();
        Workstation newWs = new Workstation(workstation.name(), workstation.status(), institutionName, existing.workstation_id());
//        workstationRepository.updateWorkstation(newWs);
        // "Put" will replace the existing workstation with the new one, keeping the key.
        jdbi.withHandle(h -> {
            WorkstationRepository workstationRepository = h.attach(WorkstationRepository.class);
            workstationRepository.updateWorkstation(newWs);
            this.workstationMap.get(existing.institution_name()).put(newWs.name(), newWs);
            return h;
        });

    }
}
