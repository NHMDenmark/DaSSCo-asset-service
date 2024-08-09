package dk.northtech.dasscoassetservice.amqp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.northtech.dasscoassetservice.domain.Acknowledge;
import dk.northtech.dasscoassetservice.services.KeycloakService;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AcknowledgeQueueListener extends QueueListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcknowledgeQueueListener.class);

    @Inject
    public AcknowledgeQueueListener(KeycloakService keycloakService, AMQPConfig amqpConfig) {
        super(keycloakService, amqpConfig, amqpConfig.acknowledgeQueueName());
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
        } catch (JsonProcessingException e) {
            throw new RuntimeException("There was an error when converting the message to an Acknowledge object.", e);
        }
    }
}
