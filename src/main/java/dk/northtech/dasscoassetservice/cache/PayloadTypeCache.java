package dk.northtech.dasscoassetservice.cache;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PayloadTypeCache {

    private Map<String, String> payloadTypeMap = new HashMap<>();

    public Map<String, String> getPayloadTypeMap() {
        return payloadTypeMap;
    }

    public void setPayloadTypeMap(Map<String, String> payloadTypeMap) {
        this.payloadTypeMap = payloadTypeMap;
    }

    public List<String> getPayloadTypes() {
        return payloadTypeMap.values().stream().toList();
    }

    public void putPayloadTypesInCache(String payloadType){
        payloadTypeMap.put(payloadType, payloadType);
    }
}
