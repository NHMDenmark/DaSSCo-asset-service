package dk.northtech.dasscoassetservice.amqp;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.rabbitmq.jms.admin.RMQConnectionFactory;
import dk.northtech.dasscoassetservice.services.KeycloakService;
import jakarta.inject.Inject;
import jakarta.jms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.time.Duration;

import static java.time.temporal.ChronoUnit.MINUTES;

@Service
public class AssetQueueListener extends QueueListener {
//    private static final Logger LOGGER = LoggerFactory.getLogger(AssetQueueListener.class);
//    private static final int RABBIT_PORT = 5672;
//    private static final int RABBIT_TLS_PORT = 5671;
//    private int qbrMax;
//    private KeycloakService keycloakService;
//    private AMQPConfig amqpConfig;
//    QueueReceiver receiver;
//    QueueSession session;
//    QueueConnection queueConnection;
//
    @Inject
    public AssetQueueListener(KeycloakService keycloakService, AMQPConfig amqpConfig) {
        super(keycloakService, amqpConfig, amqpConfig.assetQueueName());
    }
//
//    private String queueName() {
//        return this.amqpConfig.assetQueueName();
//    }
//
//    private String token() {
//        return this.keycloakService.getUserServiceToken();
//    }
//
//    private String hostname() {
//        return this.amqpConfig.host();
//    }
//
//    private boolean isSecure() {
//        return Boolean.parseBoolean(this.amqpConfig.secure());
//    }
//
//    @Override
//    protected void startUp() {
//        LOGGER.info("Initializing {}", this.getClass().getSimpleName());
//        try {
//            queueConnection = getQueueConnectionFactory().createQueueConnection("", token());
//            queueConnection.start();
//            session = queueConnection.createQueueSession(false, Session.DUPS_OK_ACKNOWLEDGE);
//            Queue queue = session.createQueue(queueName());
//            this.receiver = session.createReceiver(queue);
//        } catch (JMSException e) {
//            throw new RuntimeException("QueueListener failed to setup the connection", e);
//        }
//    }
//
//    private QueueConnectionFactory getQueueConnectionFactory(){
//        return (QueueConnectionFactory)getConnectionFactory();
//    }
//
//    private ConnectionFactory getConnectionFactory() {
//        RMQConnectionFactory rmqCF = new RMQConnectionFactory() {
//            private static final long serialVersionUID = 1L;
//            @Override
//            public Connection createConnection(String userName, String password) throws JMSException {
//                if (!isSecure()) {
//                    this.setPort(RABBIT_PORT);
//                } else {
//                    this.setPort(RABBIT_TLS_PORT);
//                }
//                return super.createConnection(userName, password);
//            }
//        };
//        if (isSecure()) {
//            try {
//                rmqCF.useSslProtocol();
//            } catch (NoSuchAlgorithmException e) {
//                throw new RuntimeException("An error occurred when trying to use SSL protocol on the RMQConnectionFactory.", e);
//            }
//        }
//        rmqCF.setHost(hostname());
//        rmqCF.setQueueBrowserReadMax(qbrMax);
//        return rmqCF;
//    }
//
//    @Override
//    protected void run() {
//        try {
//            while (isRunning()) {
////                final Semaphore sem = new Semaphore(0);
//                Message message = this.receiver.receive(Duration.of(1, MINUTES).toMillis());
//                if (message instanceof TextMessage) {
//                    handleMessage(((TextMessage) message).getText());
////                    sem.acquire();
//                }
//            }
//        } catch (Exception e) {
//            LOGGER.error("Error while receiving messages", e);
//        }
//    }

    @Override
    public void handleMessage(String message) {
        System.out.println("MESSAGE IN ASSET LISTENER:");
        System.out.println(message);
    }

//    @Override
//    protected void shutDown() {
//        LOGGER.info("Shutting down {}", this.getClass().getSimpleName());
//        try {
//            if (this.receiver != null) {
//                this.receiver.close();
//            }
//            if (this.queueConnection != null) {
//                this.queueConnection.stop();
//                this.session.close();
//            }
//        } catch (JMSException e) {
//            throw new RuntimeException(e);
//        }
//        LOGGER.info("{} is shut down", this.getClass().getSimpleName());
//    }
}
