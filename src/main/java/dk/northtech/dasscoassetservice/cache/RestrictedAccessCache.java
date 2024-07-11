package dk.northtech.dasscoassetservice.cache;

import dk.northtech.dasscoassetservice.domain.InternalRole;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RestrictedAccessCache {
    private Map<String, InternalRole> restrictedAccessMap = new HashMap<>();

    public Map<String, InternalRole> getRestrictedAccessMap() {
        return restrictedAccessMap;
    }

    public void setRestrictedAccessMap(Map<String, InternalRole> restrictedAccessMap) {
        this.restrictedAccessMap = restrictedAccessMap;
    }

    public void putRestrictedAccessInCache(String restrictedAccess){
        this.restrictedAccessMap.put(restrictedAccess, InternalRole.valueOf(restrictedAccess));
    }

    public List<InternalRole> getRestrictedAccessList(){
        return this.restrictedAccessMap.values().stream().toList();
    }

    public void clearCache(){
        this.restrictedAccessMap.clear();
    }
}
