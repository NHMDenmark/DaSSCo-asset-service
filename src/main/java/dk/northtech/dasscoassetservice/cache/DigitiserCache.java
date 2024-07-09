package dk.northtech.dasscoassetservice.cache;

import dk.northtech.dasscoassetservice.domain.Digitiser;
import dk.northtech.dasscoassetservice.domain.User;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DigitiserCache {
    private Map<String, Digitiser> digitiserMap = new HashMap<>();

    public Map<String, Digitiser> getDigitiserMap() {
        return digitiserMap;
    }

    public void setUserMap(Map<String, Digitiser> digitiserMap) {
        this.digitiserMap = digitiserMap;
    }

    public List<Digitiser> getDigitisers(){
        return digitiserMap.values().stream().toList();
    }

    public void putDigitiserInCache(Digitiser digitiser){
        digitiserMap.put(digitiser.userId, digitiser);
    }
}
