package dk.northtech.dasscoassetservice.cache;

import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.repositories.InstitutionRepository;
import org.springframework.context.ApplicationListener;
import jakarta.inject.Inject;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CacheInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private InstitutionCache institutionCache;
    private InstitutionRepository institutionRepository;
    private boolean initialized = false;

    @Inject
    public CacheInitializer(InstitutionCache institutionCache, InstitutionRepository institutionRepository){
        this.institutionCache = institutionCache;
        this.institutionRepository = institutionRepository;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event){
        if (!initialized){
            List<Institution> institutionList = institutionRepository.listInstitutions();
            for (Institution institution : institutionList){
                institutionCache.putInstitutionInCache(institution.name(), institution);
            }
            initialized = true;
        }
    }
}
