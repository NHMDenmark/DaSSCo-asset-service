package dk.northtech.dasscoassetservice.cache;

import dk.northtech.dasscoassetservice.domain.Collection;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CollectionCache {
    private Map<String, Collection> collectionMap;

    public Map<String, Collection> getCollectionMap() {
        return collectionMap;
    }

    public void setCollectionMap(Map<String, Collection> collectionMap) {
        this.collectionMap = collectionMap;
    }


}
