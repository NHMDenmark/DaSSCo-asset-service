package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.PublicAsset;
import dk.northtech.dasscoassetservice.repositories.PublicAssetRepository;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PublicAssetService {
    private final Jdbi jdbi;


    public PublicAssetService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }


    public Optional<PublicAsset> getAsset(String asset_guid) {
        return this.jdbi.withHandle(h -> {
            PublicAssetRepository repo = h.attach(PublicAssetRepository.class);
            return repo.readPublicAsset(asset_guid);
        });
    }
}
