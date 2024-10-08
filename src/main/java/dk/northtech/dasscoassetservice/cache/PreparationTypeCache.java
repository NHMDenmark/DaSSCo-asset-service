package dk.northtech.dasscoassetservice.cache;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PreparationTypeCache {
    private final ConcurrentHashMap<String, String> preparationTypeMap = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, String> getPreparationTypeMap() {
        return preparationTypeMap;
    }

    public List<String> getPreparationTypes(){
        return preparationTypeMap.values().stream().toList();
    }

    public void putPreparationTypesInCacheIfAbsent(String preparationType){
        preparationTypeMap.putIfAbsent(preparationType, preparationType);
    }

    public void clearCache(){
        this.preparationTypeMap.clear();
    }
}
