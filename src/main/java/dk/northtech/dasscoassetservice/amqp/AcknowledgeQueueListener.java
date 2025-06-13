package dk.northtech.dasscoassetservice.amqp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.northtech.dasscoassetservice.domain.Acknowledge;
import dk.northtech.dasscoassetservice.domain.AcknowledgeStatus;
import dk.northtech.dasscoassetservice.services.AssetSyncService;
import dk.northtech.dasscoassetservice.services.KeycloakService;
import dk.northtech.dasscoassetservice.services.UserService;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AcknowledgeQueueListener extends QueueListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcknowledgeQueueListener.class);
    private final AssetSyncService assetSyncService;

    @Inject
    public AcknowledgeQueueListener(KeycloakService keycloakService, AMQPConfig amqpConfig, AssetSyncService assetSyncService) {
        super(keycloakService, amqpConfig, amqpConfig.acknowledgeQueueName());
        this.assetSyncService = assetSyncService;
    }

    @Override
    public void handleMessage(String message) {
        System.out.println("MESSAGE IN ACKNOWLEDGE LISTENER:");
        System.out.println(message);
        try {
            ObjectReader or = new ObjectMapper().registerModule(new JavaTimeModule()).readerFor(Acknowledge.class);
            Acknowledge acknowledge = or.readValue(message);
            System.out.println("received the ack object:");
            System.out.println(acknowledge);
//            if (acknowledge.status().equals(AcknowledgeStatus.SUCCESS)) {
            this.assetSyncService.handleAcknowledge(acknowledge, "service-user");
//            } else {
//                System.out.println("bruh status is weird, it's: " + acknowledge.status());
//            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("There was an error when converting the message to an Acknowledge object.", e);
        }
    }
}
