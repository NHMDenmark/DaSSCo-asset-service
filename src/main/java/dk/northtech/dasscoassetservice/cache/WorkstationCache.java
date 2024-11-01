package dk.northtech.dasscoassetservice.cache;

import dk.northtech.dasscoassetservice.domain.Workstation;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class WorkstationCache {

    private final ConcurrentHashMap<String, Workstation> workstationMap = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, Workstation> getWorkstationMap() {
        return workstationMap;
    }

    public List<Workstation> getWorkstations(String institution){
        return workstationMap.values().stream()
                .filter(workstation -> institution.equals(workstation.institution_name()))
                .collect(Collectors.toList());
    }

    public void putWorkstationInCacheIfAbsent(Workstation workstation){
        this.workstationMap.putIfAbsent(workstation.name(), workstation);
    }

    public void putWorkstationInCache(Workstation workstation){
        this.workstationMap.put(workstation.name(), workstation);
    }

    public Workstation workstationExists(String workstationName) {
        return workstationMap.get(workstationName);
    }
}
