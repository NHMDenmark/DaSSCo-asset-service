package dk.northtech.dasscoassetservice.cache;

import dk.northtech.dasscoassetservice.domain.Collection;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CollectionCache {
    private Map<String, Collection> collectionMap = new HashMap<>();

    public Map<String, Collection> getCollectionMap() {
        return collectionMap;
    }

    public void setCollectionMap(Map<String, Collection> collectionMap) {
        this.collectionMap = collectionMap;
    }

    public void putCollectionInCache(String institutionName, String collectionName, Collection collection){
        this.collectionMap.put(institutionName + "." + collectionName, collection);
    }
}
