package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.repositories.AssetSyncRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssetSyncService {
    private final Jdbi jdbi;

    @Inject
    public AssetSyncService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public List<Asset> getAllCompletedAssets() {
        return jdbi.onDemand(AssetSyncRepository.class).getAllCompletedAssets(false);
    }

    public List<Asset> getAllUnsyncedCompletedAssets() {
        return jdbi.onDemand(AssetSyncRepository.class).getAllCompletedAssets(true);
    }

    public List<String> setAssetsSynced(List<String> assetGuids) {
        return jdbi.onDemand(AssetSyncRepository.class).setAssetsSynced(assetGuids);
    }
}
