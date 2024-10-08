package dk.northtech.dasscoassetservice.cache;

import dk.northtech.dasscoassetservice.domain.Digitiser;
import dk.northtech.dasscoassetservice.domain.User;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DigitiserCache {
    private final ConcurrentHashMap<String, Digitiser> digitiserMap = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, Digitiser> getDigitiserMap() {
        return digitiserMap;
    }

    public List<Digitiser> getDigitisers(){
        return digitiserMap.values().stream().toList();
    }

    public void putDigitiserInCacheIfAbsent(Digitiser digitiser){
        digitiserMap.putIfAbsent(digitiser.userId(), digitiser);
    }
}
