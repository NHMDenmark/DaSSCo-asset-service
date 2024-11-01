package dk.northtech.dasscoassetservice.cache;

import dk.northtech.dasscoassetservice.domain.InternalRole;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RestrictedAccessCache {
    private final ConcurrentHashMap<String, InternalRole> restrictedAccessMap = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, InternalRole> getRestrictedAccessMap() {
        return restrictedAccessMap;
    }

    public void putRestrictedAccessInCacheIfAbsent(String restrictedAccess){
        this.restrictedAccessMap.putIfAbsent(restrictedAccess, InternalRole.valueOf(restrictedAccess));
    }

    public List<InternalRole> getRestrictedAccessList(){
        return this.restrictedAccessMap.values().stream().toList();
    }

    public void clearCache(){
        this.restrictedAccessMap.clear();
    }
}
