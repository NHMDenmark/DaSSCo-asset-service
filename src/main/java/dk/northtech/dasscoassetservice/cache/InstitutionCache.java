package dk.northtech.dasscoassetservice.cache;

import dk.northtech.dasscoassetservice.domain.Institution;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InstitutionCache {
    private Map<String, Institution> institutionMap = new HashMap<>();

    public Map<String, Institution> getInstitutionMap() {
        return institutionMap;
    }

    public void setInstitutionMap(Map<String, Institution> institutionCache) {
        this.institutionMap = institutionCache;
    }

    public void putInstitutionInCache(String institutionName, Institution institution) {
        institutionMap.put(institutionName, institution);
    }

    public Collection<Institution> getInstitutions(){
        return institutionMap.values();
    }

    public boolean institutionExists(String id){
        return institutionMap.containsKey(id);
    }
}
