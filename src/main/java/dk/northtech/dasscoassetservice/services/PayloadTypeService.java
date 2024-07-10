package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.cache.PayloadTypeCache;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PayloadTypeService {

    private PayloadTypeCache payloadTypeCache;

    @Inject
    public PayloadTypeService(PayloadTypeCache payloadTypeCache){
        this.payloadTypeCache = payloadTypeCache;
    }

    public List<String> listPayloadTypes(){
        return payloadTypeCache.getPayloadTypes();
    }
}
