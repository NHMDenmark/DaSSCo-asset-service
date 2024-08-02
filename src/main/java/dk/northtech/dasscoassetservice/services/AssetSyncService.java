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

    public List<Asset> synchroniseAllAssets() {
        return jdbi.onDemand(AssetSyncRepository.class).getAllCompletedAssets(false);
    }

    public List<Asset> synchroniseUnsyncedAssets() {
        return jdbi.onDemand(AssetSyncRepository.class).getAllCompletedAssets(true);
    }

    public List<String> setAssetsSynced(List<Asset> assetGuids) {
        String formattedAssetGuids = assetGuids.stream().map(s -> "'" + s + "'").toList().toString();

        return jdbi.onDemand(AssetSyncRepository.class).setAssetsSynced(formattedAssetGuids);
    }
}
