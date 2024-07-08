package dk.northtech.dasscoassetservice.cache;

import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.repositories.CollectionRepository;
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
    private CollectionCache collectionCache;
    private CollectionRepository collectionRepository;
    private boolean initialized = false;

    @Inject
    public CacheInitializer(InstitutionCache institutionCache, InstitutionRepository institutionRepository,
                            CollectionCache collectionCache, CollectionRepository collectionRepository){
        this.institutionCache = institutionCache;
        this.institutionRepository = institutionRepository;
        this.collectionCache = collectionCache;
        this.collectionRepository = collectionRepository;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event){
        if (!initialized){
            List<Institution> institutionList = institutionRepository.listInstitutions();
            if (!institutionList.isEmpty()){
                for (Institution institution : institutionList){
                    institutionCache.putInstitutionInCache(institution.name(), institution);
                    List<Collection> collectionList = collectionRepository.listCollections(institution);
                    if (!collectionList.isEmpty()){
                        for (Collection collection : collectionList){
                            this.collectionCache.putCollectionInCache(institution.name(), collection.name(), collection);
                        }
                    }
                }
            }
            initialized = true;
        }
    }
}
