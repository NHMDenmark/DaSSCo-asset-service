package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Funding;
import dk.northtech.dasscoassetservice.domain.User;
import dk.northtech.dasscoassetservice.repositories.FundingRepository;
import dk.northtech.dasscoassetservice.repositories.UserRepository;
import jakarta.inject.Inject;
import joptsimple.internal.Strings;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FundingService {

    private Jdbi jdbi;
    private boolean initialised;
    private static final Logger logger = LoggerFactory.getLogger(FundingService.class);
    private ConcurrentHashMap<String, Funding> fundingMap = new ConcurrentHashMap<>();
    @Inject
    public FundingService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public void forceRefreshCache() {
        initFunding(true);
    }
    public void initFunding() {
        initFunding(false);
    }
    public Funding ensureExists(String funding) {
        if(Strings.isNullOrEmpty(funding)) {
            throw new RuntimeException("User was not found");
        }
        Optional<Funding> userIfExists = getFundingIfExists(funding);
        if(userIfExists.isPresent()){
            Funding existing = userIfExists.get();

            return existing;
        }
        Funding persistFunding = persistFunding(funding);
        return persistFunding;
    }

    public Funding persistFunding(String funding) {
        if(Strings.isNullOrEmpty(funding)) {
            throw new IllegalArgumentException("Funding cannot be null");
        }
        if(!this.initialised) {
            initFunding();
        }
        if(this.fundingMap.containsKey(funding)){
            throw new IllegalArgumentException("Funding already exists");
        }
        return jdbi.withHandle(h->{
            FundingRepository attach = h.attach(FundingRepository.class);
            Funding persistedFunding = attach.insertFunding(funding);
            fundingMap.put(persistedFunding.funding(), persistedFunding);
            return persistedFunding;
        });
        // return orig user as it may have keycloak token for later use.
    }


    public Optional<Funding> getFundingIfExists(String funding_name) {
        if(!this.initialised) {
            initFunding();
        }
        Funding funding = fundingMap.get(funding_name);
        if(funding == null) {
            return Optional.empty();
        }
        return Optional.of(funding);
    }
    private void initFunding(boolean force) {
        synchronized (this) {
            if (!this.initialised || force) {
                jdbi.withHandle(h -> {
                    FundingRepository fundingRepository = h.attach(FundingRepository.class);
                    List<Funding> funds = fundingRepository.listFunds();
                    this.fundingMap.clear();
                    for(Funding funding: funds) {
                        this.fundingMap.put(funding.funding(), funding);
                    }
                    logger.info("Loaded {} funds", fundingMap.size());
                    return h;
                });
                this.initialised = true;
            }
        }
    }
}
