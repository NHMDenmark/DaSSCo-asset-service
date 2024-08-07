package dk.northtech.dasscoassetservice.configuration;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ServiceManager;
import dk.northtech.dasscoassetservice.amqp.QueueBroadcaster;
import dk.northtech.dasscoassetservice.amqp.QueueListener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Services {
    private static final Logger LOGGER = LoggerFactory.getLogger(Services.class);
    private final ServiceManager serviceManager;

    public Services(QueueBroadcaster queueBroadcaster, QueueListener queueListener) {
        this.serviceManager = new ServiceManager(ImmutableList.of(queueBroadcaster, queueListener));
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
        serviceManager.awaitStopped();
        LOGGER.info("Services shut down");
    }
}
