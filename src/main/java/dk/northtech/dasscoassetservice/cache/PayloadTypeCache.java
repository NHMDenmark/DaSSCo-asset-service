package dk.northtech.dasscoassetservice.cache;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PayloadTypeCache {

    private final ConcurrentHashMap<String, String> payloadTypeMap = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, String> getPayloadTypeMap() {
        return payloadTypeMap;
    }

    public List<String> getPayloadTypes() {
        return payloadTypeMap.values().stream().toList();
    }

    public void putPayloadTypesInCacheIfAbsent(String payloadType){
        payloadTypeMap.putIfAbsent(payloadType, payloadType);
    }

    public void clearCache(){
        this.payloadTypeMap.clear();
    }
}
