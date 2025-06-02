package dk.northtech.dasscoassetservice.amqp;

import dk.northtech.dasscoassetservice.services.KeycloakService;
import dk.northtech.dasscoassetservice.services.SpecifyAdapterClient;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

@Service
public class AssetQueueListener extends QueueListener {
    private final SpecifyAdapterClient specifyAdapterClient;

    @Inject
    public AssetQueueListener(KeycloakService keycloakService, AMQPConfig amqpConfig, SpecifyAdapterClient specifyAdapterClient) {
        super(keycloakService, amqpConfig, amqpConfig.assetQueueName());
        this.specifyAdapterClient = specifyAdapterClient;
    }

    @Override
    public void handleMessage(String message) {
        System.out.println("MESSAGE IN ASSET LISTENER:");
        System.out.println(message);
        int statusCode = this.specifyAdapterClient.sendAssets(message);
        System.out.println("sent them off and got status code: " + statusCode);
    }
}
