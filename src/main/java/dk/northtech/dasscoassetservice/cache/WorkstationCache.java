package dk.northtech.dasscoassetservice.cache;

import dk.northtech.dasscoassetservice.domain.Workstation;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class WorkstationCache {

    private Map<String, Workstation> workstationMap = new HashMap<>();

    public Map<String, Workstation> getWorkstationMap() {
        return workstationMap;
    }

    public void setWorkstationMap(Map<String, Workstation> workstationMap) {
        this.workstationMap = workstationMap;
    }

    public List<Workstation> getWorkstations(String institution){
        return workstationMap.values().stream()
                .filter(workstation -> institution.equals(workstation.institution_name()))
                .collect(Collectors.toList());
    }

    public void putWorkstationInCache(Workstation workstation){
        this.workstationMap.put(workstation.name(), workstation);
    }

    public Workstation workstationExists(String workstationName) {
        return workstationMap.get(workstationName);
    }
}
