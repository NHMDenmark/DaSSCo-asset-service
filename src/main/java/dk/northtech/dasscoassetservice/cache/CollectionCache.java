package dk.northtech.dasscoassetservice.cache;

import dk.northtech.dasscoassetservice.domain.Collection;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class CollectionCache {
    private final ConcurrentHashMap<String, Collection> collectionMap = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, Collection> getCollectionMap() {
        return collectionMap;
    }

    public void putCollectionInCacheIfAbsent(String institutionName, String collectionName, Collection collection){
        this.collectionMap.putIfAbsent(institutionName + "." + collectionName, collection);
    }

    public void put(String institutionName, String collectionName, Collection collection){
        this.collectionMap.put(institutionName + "." + collectionName, collection);
    }

    public List<Collection> getCollections(String institution){
        return collectionMap.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(institution + "."))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }
}
