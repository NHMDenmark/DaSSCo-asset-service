package dk.northtech.dasscoassetservice.cache;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PreparationTypeCache {

    private Map<String, String> preparationTypeMap = new HashMap<>();

    public Map<String, String> getPreparationType() {
        return preparationTypeMap;
    }

    public void setPreparationType(Map<String, String> preparationType) {
        this.preparationTypeMap = preparationType;
    }

    public List<String> getPreparationTypes(){
        return preparationTypeMap.values().stream().toList();
    }

    public void putPreparationTypesInCache(String preparationType){
        preparationTypeMap.put(preparationType, preparationType);
    }

    public void clearCache(){
        this.preparationTypeMap.clear();
    }
}
