package dk.northtech.dasscoassetservice.cache;

import dk.northtech.dasscoassetservice.domain.Institution;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InstitutionCache {
    private final ConcurrentHashMap<String, Institution> institutionMap = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, Institution> getInstitutionMap() {
        return institutionMap;
    }

    public void putInstitutionInCacheIfAbsent(String institutionName, Institution institution) {
        institutionMap.putIfAbsent(institutionName, institution);
    }

    public List<Institution> getInstitutions() {
        return institutionMap.values().stream().toList();
    }

    public boolean institutionExists(String id) {
        return institutionMap.containsKey(id);
    }

    public Institution getInstitution(String institutionName) {
        return this.institutionMap.get(institutionName);
    }
}
