package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.amqp.QueueBroadcaster;
import dk.northtech.dasscoassetservice.domain.Acknowledge;
import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.InternalStatus;
import dk.northtech.dasscoassetservice.repositories.AssetRepository;
import dk.northtech.dasscoassetservice.repositories.AssetSyncRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class AssetSyncService {
    private final Jdbi jdbi;
    private final QueueBroadcaster queueBroadcaster;
    private static final Logger LOGGER = LoggerFactory.getLogger(AssetSyncService.class);
    private final AssetService assetService;
    @Inject
    public AssetSyncService(Jdbi jdbi, QueueBroadcaster queueBroadcaster, @Lazy AssetService assetService) {
        this.jdbi = jdbi;
        this.assetService = assetService;
        this.queueBroadcaster = queueBroadcaster;
    }

//    public void send(Asset asset) {
//        ObjectWriter ow = new ObjectMapper().registerModule(new JavaTimeModule()).writer().withDefaultPrettyPrinter();
//        try {
//            String json = ow.writeValueAsString(asset);
//            this.queueBroadcaster.sendMessage(json);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//    }

//    public void sendAllAssetsToQueue(boolean unsyncedOnly) {
//        List<Asset> completedAssets;
//        if (unsyncedOnly) {
//            completedAssets = getAllUnsyncedCompletedAssets();
//        } else {
//            completedAssets = getAllCompletedAssets();
//        }
//
//        this.queueBroadcaster.sendAssets(completedAssets);
//    }

    public void sendAssetToQueue(Asset asset) {
        this.queueBroadcaster.sendAssets(asset);
    }

    public Optional<Acknowledge> handleAcknowledge(Acknowledge acknowledge, String username) {
        return jdbi.onDemand(AssetSyncRepository.class).persistAcknowledge(acknowledge, username);
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

    @Scheduled(cron = "0 0 3 * * *")
    public void syncAssets() {
        jdbi.withHandle(h -> {
            AssetSyncRepository assetSyncRepository = h.attach(AssetSyncRepository.class);
            AssetRepository assetRepository = h.attach(AssetRepository.class);

            Set<String> assetsForSpecifySync = assetSyncRepository.findAssetsForSpecifySync();
            for (String assetGuid : assetsForSpecifySync) {
                Optional<Asset> assetOpt = assetService.getAsset(assetGuid);
                if (assetOpt.isPresent()) {
                    Asset asset = assetOpt.get();
                    sendAssetToQueue(asset);
                    asset.internal_status = InternalStatus.SPECIFY_SYNC_SCHEDULED;
                    assetRepository.updateAssetNoEvent(asset);
                }
            }
            return h;
        });
    }

    public void syncAsset() {
        jdbi.withHandle(h -> {
            AssetSyncRepository assetSyncRepository = h.attach(AssetSyncRepository.class);
            AssetRepository assetRepository = h.attach(AssetRepository.class);

            Set<String> assetsForSpecifySync = assetSyncRepository.findAssetsForSpecifySync();
            for (String assetGuid : assetsForSpecifySync) {
                Optional<Asset> assetOpt = assetService.getAsset(assetGuid);
                if (assetOpt.isPresent()) {
                    Asset asset = assetOpt.get();
                    sendAssetToQueue(asset);
                    asset.internal_status = InternalStatus.SPECIFY_SYNC_SCHEDULED;
                    assetRepository.updateAssetNoEvent(asset);
                }
            }
            return h;
        });
    }

    public void checkAndSync(Asset asset) {
        if (asset.asset_locked && asset.push_to_specify && InternalStatus.ERDA_SYNCHRONISED.equals(asset.internal_status)) {
            try {
                sendAssetToQueue(asset);
                asset.internal_status = InternalStatus.SPECIFY_SYNC_SCHEDULED;
                jdbi.onDemand(AssetRepository.class).updateAssetNoEvent(asset);
            } catch (Exception e) {
                // This is likely to be an internal DASSCO error
                // Do not set a status, this should be
                LOGGER.error("Failed to send to queue", e);
            }
        }
    }
}
