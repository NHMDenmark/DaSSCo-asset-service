package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.amqp.QueueBroadcaster;
import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.domain.specifyarssync.SpecifyArsSyncMessage;
import dk.northtech.dasscoassetservice.domain.specifyarssync.SpecifySyncStatus;
import dk.northtech.dasscoassetservice.domain.specifyarssync.SyncAcknowledge;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
public class SpecifyArsSyncService {
    private static final Logger log = LoggerFactory.getLogger(SpecifyArsSyncService.class);
    private final AssetService assetService;
    private final UserService userService;
    private final SpecimenService specimenService;
    private final PipelineService pipelineService;
    private final QueueBroadcaster queueBroadcaster;

    @Inject
    public SpecifyArsSyncService(AssetService assetService, UserService userService, SpecimenService specimenService, PipelineService pipelineService, QueueBroadcaster queueBroadcaster) {
        this.assetService = assetService;
        this.userService = userService;
        this.specimenService = specimenService;
        this.pipelineService = pipelineService;
        this.queueBroadcaster = queueBroadcaster;
    }

    public void handleSpecifyUpdate(SpecifyArsSyncMessage specifyArsSyncMessage) {
        try {
            Asset specifyAsset = specifyArsSyncMessage.asset;
            Optional<Asset> existing = assetService.getAsset(specifyAsset.asset_guid);
            User user = new User("dassco-asset-service");
            user = userService.ensureExists(user);
            specifyAsset.digitiser = user.username;
            if (existing.isPresent()) {
                mapAsset(existing, specifyArsSyncMessage);
            } else {
                specifyAsset.pipeline = specifyAsset.pipeline == null ? "unknown" : specifyAsset.pipeline;
                log.info("pipeline is {}", specifyAsset.pipeline);
                log.info("institution is {}", specifyAsset.institution);
                if (pipelineService.findPipelineByInstitutionAndName(specifyAsset.pipeline, specifyAsset.institution).isEmpty()) {
                    pipelineService.persistPipeline(new Pipeline("unknown", specifyAsset.institution), specifyAsset.institution);
                }
                for (AssetSpecimen specimen : specifyArsSyncMessage.asset.asset_specimen) {
                    specimenService.putSpecimen(specimen.specimen, user);
                }
                assetService.persistAsset(specifyAsset, user, 122, false);
                queueBroadcaster.sendSpecifyArsAcknowledge(new SyncAcknowledge(SpecifySyncStatus.STARTED, specifyArsSyncMessage.specifySyncLogId, null));
            }
        } catch (Exception e1) {
            queueBroadcaster.sendSpecifyArsAcknowledge(new SyncAcknowledge(SpecifySyncStatus.FAILED, specifyArsSyncMessage.specifySyncLogId, e1.getMessage()));
        }

    }

    private void mapAsset(Optional<Asset> existing, SpecifyArsSyncMessage specifyArsSyncMessage) {

    }
}
