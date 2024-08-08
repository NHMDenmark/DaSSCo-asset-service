package dk.northtech.dasscoassetservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.northtech.dasscoassetservice.amqp.QueueBroadcaster;
import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.repositories.AssetSyncRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AssetSyncService {
    private final Jdbi jdbi;
    private final QueueBroadcaster queueBroadcaster;

    @Inject
    public AssetSyncService(Jdbi jdbi, QueueBroadcaster queueBroadcaster) {
        this.jdbi = jdbi;
        this.queueBroadcaster = queueBroadcaster;
    }

    public void send(Asset asset) {
        ObjectWriter ow = new ObjectMapper().registerModule(new JavaTimeModule()).writer().withDefaultPrettyPrinter();
        try {
            String json = ow.writeValueAsString(asset);
            this.queueBroadcaster.sendMessage(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void syncAssets(boolean unsyncedOnly) {
        List<Asset> completedAssets = new ArrayList<>();
        if (unsyncedOnly) completedAssets = getAllUnsyncedCompletedAssets();
        else completedAssets = getAllCompletedAssets();

        ObjectWriter ow = new ObjectMapper().registerModule(new JavaTimeModule()).writer().withDefaultPrettyPrinter();
        try {
            String json = ow.writeValueAsString(completedAssets);
            this.queueBroadcaster.sendMessage(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("An error occurred when trying to turn the Assets into JSON for the queue.", e);
        }
        // todo handle return of queue and set synced property
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
