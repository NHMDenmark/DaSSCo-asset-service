package dk.northtech.dasscoassetservice.cache;

import dk.northtech.dasscoassetservice.domain.AssetStatus;
import dk.northtech.dasscoassetservice.domain.AssetStatusInfo;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StatusCache {
    private Map<String, AssetStatus> statusMap = new HashMap<>();

    public Map<String, AssetStatus> getStatusMap() {
        return statusMap;
    }

    public void setStatusMap(Map<String, AssetStatus> statusMap) {
        this.statusMap = statusMap;
    }

    public List<AssetStatus> getStatus() {
        return statusMap.values().stream().toList();
    }

    public void putStatusInCache(AssetStatus assetStatus){
        statusMap.put(assetStatus.name(), assetStatus);
    }
}
