package dk.northtech.dasscoassetservice.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.northtech.dasscoassetservice.domain.Acknowledge;
import dk.northtech.dasscoassetservice.domain.specifyarssync.SpecifyArsSyncMessage;
import dk.northtech.dasscoassetservice.services.AssetService;
import dk.northtech.dasscoassetservice.services.KeycloakService;
import dk.northtech.dasscoassetservice.services.SpecifyAdapterClient;
import dk.northtech.dasscoassetservice.services.SpecifyArsSyncService;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SpecifyUpdateQueueListener extends QueueListener {
    private final ObjectReader or = new ObjectMapper().registerModule(new JavaTimeModule()).readerFor(Acknowledge.class);
    private final SpecifyArsSyncService specifyArsSyncService;
    @Inject
    public SpecifyUpdateQueueListener(KeycloakService keycloakService, SpecifyArsSyncService specifyArsSyncService, AMQPConfig amqpConfig) {
        super(keycloakService, amqpConfig, amqpConfig.specifyArsSyncQueueName());
        this.specifyArsSyncService = specifyArsSyncService;
    }

    @Override
    public void handleMessage(String message) {
        System.out.println("MESSAGE IN ASSET LISTENER:");
        System.out.println(message);
        try {
            SpecifyArsSyncMessage specifyArsSyncMessage = or.readValue(message, SpecifyArsSyncMessage.class);
            specifyArsSyncService.handleSpecifyUpdate(specifyArsSyncMessage);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Q message ",e);
        }
    }
}
