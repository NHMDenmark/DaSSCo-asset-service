package dk.northtech.dasscoassetservice.amqp;

import dk.northtech.dasscoassetservice.services.AssetService;
import dk.northtech.dasscoassetservice.services.KeycloakService;
import dk.northtech.dasscoassetservice.services.SpecifyAdapterClient;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

@Service
public class AssetQueueListener extends QueueListener {
    private final SpecifyAdapterClient specifyAdapterClient;
    private final AssetService assetService;
    @Inject
    public AssetQueueListener(KeycloakService keycloakService, AssetService assetService, AMQPConfig amqpConfig, SpecifyAdapterClient specifyAdapterClient) {
        super(keycloakService, amqpConfig, amqpConfig.assetQueueName());
        this.specifyAdapterClient = specifyAdapterClient;
        this.assetService = assetService;
    }

    @Override
    public void handleMessage(String message) {
        System.out.println("MESSAGE IN ASSET LISTENER:");
        System.out.println(message);
        int statusCode = this.specifyAdapterClient.sendAssets(message);
        System.out.println("sent them off and got status code: " + statusCode);
    }
}
