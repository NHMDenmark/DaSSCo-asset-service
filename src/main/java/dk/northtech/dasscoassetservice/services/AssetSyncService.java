package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.amqp.QueueBroadcaster;
import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.repositories.AssetRepository;
import dk.northtech.dasscoassetservice.repositories.AssetSyncRepository;
import dk.northtech.dasscoassetservice.repositories.EventRepository;
import dk.northtech.dasscoassetservice.repositories.FileRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class AssetSyncService {
    private final Jdbi jdbi;
    private final QueueBroadcaster queueBroadcaster;
    private static final Logger LOGGER = LoggerFactory.getLogger(AssetSyncService.class);
    private final AssetService assetService;
    private final PipelineService pipelineService;
    private final UserService userService;
    @Inject
    public AssetSyncService(Jdbi jdbi, QueueBroadcaster queueBroadcaster, @Lazy AssetService assetService, PipelineService pipelineService, UserService userService) {
        this.jdbi = jdbi;
        this.assetService = assetService;
        this.queueBroadcaster = queueBroadcaster;
        this.pipelineService = pipelineService;
        this.userService = userService;
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

    public void sendAssetToQueue(ARSUpdate arsUpdate) {
        this.queueBroadcaster.sendAssets(arsUpdate);
    }

    public void handleAcknowledge(Acknowledge acknowledge) {
        jdbi.inTransaction(handle -> {
            String error_message = null;
            InternalStatus status = InternalStatus.SPECIFY_SYNCHRONISED;
            if (acknowledge == null) {
                LOGGER.error("acknowledge is null");
                return handle;
            } else if (acknowledge.status() != AcknowledgeStatus.SUCCESS) {
                error_message = acknowledge.status().toString() + " " + acknowledge.message();
                status = InternalStatus.SPECIFY_SYNC_FAILED;
            }
            AssetRepository assetRepository = handle.attach(AssetRepository.class);
            Optional<Asset> assetOpt = assetService.getAsset(acknowledge.asset_guid());
            if (assetOpt.isPresent()) {
                Asset asset = assetOpt.get();
                asset.internal_status = status;
                asset.error_message = error_message;
                assetRepository.updateAssetNoEventInternal(asset);
                if (acknowledge.status() == AcknowledgeStatus.SUCCESS) {
                    FileRepository fileRepository = handle.attach(FileRepository.class);
                    for(DasscoFile file: acknowledge.updatedFiles()) {
                        if(file.specifyAttachmentId() != null) {
                            fileRepository.setSpecifyAttachmentId(file.fileId(), file.specifyAttachmentId());
                        }
                    }
                    EventRepository attach = handle.attach(EventRepository.class);
                    // Insert event with info about the user that caused the sync
                    Event event = null;
                    for (Event assetEvent : asset.events) {
                        if ((assetEvent.event == DasscoEvent.UPDATE_ASSET_METADATA || assetEvent.event == DasscoEvent.BULK_UPDATE_ASSET_METADATA)
                            && (event == null || event.timestamp.isBefore(assetEvent.timestamp))
                        ) {
                            event = assetEvent;
                        }
                    }
                    if (event != null) {
                        Optional<Pipeline> pipelineByInstitutionAndName = pipelineService.findPipelineByInstitutionAndName(asset.institution, event.pipeline);
                        Optional<User> userIfExists = userService.getUserIfExists(event.user);
                        if(userIfExists.isPresent()) {
                            Integer dasscoUserId = userIfExists.get().dassco_user_id;
                            attach.insertEvent(asset.asset_guid, DasscoEvent.SYNCHRONISE_SPECIFY, dasscoUserId, pipelineByInstitutionAndName.map(Pipeline::pipeline_id).orElse(null));
                        }
                    }
                }
            }
            return handle;
        });
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
                    sendAssetToQueue(new ARSUpdate(asset));
                    asset.internal_status = InternalStatus.SPECIFY_SYNC_SCHEDULED;
                    assetRepository.updateAssetNoEvent(asset);
                }
            }
            return h;
        });
    }

    public void syncAsset(String guid) {
        LOGGER.info("Syncing asset {}", guid);
        jdbi.withHandle(h -> {
            AssetRepository assetRepository = h.attach(AssetRepository.class);
            Optional<Asset> assetOpt = assetService.getAsset(guid);
            if (assetOpt.isPresent()) {
                Asset asset = assetOpt.get();
                FileRepository fileRepository = h.attach(FileRepository.class);
                fileRepository.getFilesByAssetGuid(asset.asset_guid);
                sendAssetToQueue(new ARSUpdate(asset));
                asset.internal_status = InternalStatus.SPECIFY_SYNC_SCHEDULED;
                assetRepository.updateAssetNoEvent(asset);
            }

            return h;
        });
    }

    public void checkAndSync(Asset asset) {
        if (asset.asset_locked && asset.push_to_specify && InternalStatus.ERDA_SYNCHRONISED.equals(asset.internal_status)) {
            try {
                sendAssetToQueue(new ARSUpdate(asset));
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
