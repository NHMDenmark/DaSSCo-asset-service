package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.cache.DigitiserCache;
import dk.northtech.dasscoassetservice.domain.Digitiser;
import dk.northtech.dasscoassetservice.repositories.DigitiserRepository;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DigitiserService {

    private DigitiserRepository userRepository;
    private DigitiserCache digitiserCache;

    @Inject
    public DigitiserService(DigitiserCache userCache){
        this.digitiserCache = userCache;
    }

    public List<Digitiser> listDigitisers(){
        return digitiserCache.getDigitisers();
    }

}
