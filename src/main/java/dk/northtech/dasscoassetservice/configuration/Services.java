package dk.northtech.dasscoassetservice.configuration;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ServiceManager;
import dk.northtech.dasscoassetservice.amqp.AcknowledgeQueueListener;
import dk.northtech.dasscoassetservice.amqp.QueueBroadcaster;
import dk.northtech.dasscoassetservice.amqp.AssetQueueListener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class Services {
    private static final Logger LOGGER = LoggerFactory.getLogger(Services.class);
    private final ServiceManager serviceManager;
    private final AcknowledgeQueueListener acknowledgeQueueListener;
//    private final AssetQueueListener assetQueueListener;

    public Services(QueueBroadcaster queueBroadcaster, AcknowledgeQueueListener acknowledgeQueueListener) {
        this.acknowledgeQueueListener = acknowledgeQueueListener;
//        this.assetQueueListener = assetQueueListener;
        this.serviceManager = new ServiceManager(ImmutableList.of(queueBroadcaster, acknowledgeQueueListener));
    }

    @PostConstruct
    public void startup() {
        LOGGER.info("Services init");
        this.serviceManager.startAsync();
        this.serviceManager.awaitHealthy();
        LOGGER.info("Services running");
    }

    @PreDestroy
    public void teardown() {
        LOGGER.info("Services teardown");
        serviceManager.stopAsync();
//        try {
//            serviceManager.awaitStopped(2, TimeUnit.SECONDS);
//        } catch (TimeoutException e) {
//            throw new RuntimeException(e);
//        }
        serviceManager.awaitStopped();
        LOGGER.info("Services shut down");
    }
}
