package dk.northtech.dasscoassetservice.cache;

import dk.northtech.dasscoassetservice.domain.AssetStatus;
import dk.northtech.dasscoassetservice.domain.AssetStatusInfo;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StatusCache {
    private final ConcurrentHashMap<String, AssetStatus> statusMap = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, AssetStatus> getStatusMap() {
        return statusMap;
    }

    public List<AssetStatus> getStatus() {
        return statusMap.values().stream().toList();
    }

    public void putStatusInCacheIfAbsent(AssetStatus assetStatus){
        statusMap.putIfAbsent(assetStatus.name(), assetStatus);
    }

    public void clearCache(){
        this.statusMap.clear();
    }
}
