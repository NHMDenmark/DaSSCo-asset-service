package dk.northtech.dasscoassetservice.amqp;

import com.google.common.util.concurrent.AbstractIdleService;
import com.rabbitmq.jms.admin.RMQConnectionFactory;
import dk.northtech.dasscoassetservice.services.KeycloakService;
import jakarta.inject.Inject;
import jakarta.jms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;

@Service
public class QueueBroadcaster extends AbstractIdleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueBroadcaster.class);
    private static final int RABBIT_PORT = 5672;
    private static final int RABBIT_TLS_PORT = 5671;
    private int qbrMax;
    private KeycloakService keycloakService;
    private AMQPConfig amqpConfig;
    QueueSender sender;
    QueueSession session;
    QueueConnection queueConnection;

    @Inject
    public QueueBroadcaster(KeycloakService keycloakService, AMQPConfig amqpConfig) {
        this.keycloakService = keycloakService;
        this.amqpConfig = amqpConfig;
        this.qbrMax = 0;
    }

    private String queueName() {
        return this.amqpConfig.queueName();
    }

    private String token() {
        return this.keycloakService.getUserServiceToken();
    }

    private String hostname() {
        return this.amqpConfig.host();
    }

    private boolean isSecure() {
        return Boolean.parseBoolean(this.amqpConfig.secure());
    }

    @Override
    protected void startUp() {
        LOGGER.info("Initializing {}", this.getClass().getSimpleName());
        try {
            System.out.println("IT'S ALL HAPPENING");
            queueConnection = getQueueConnectionFactory().createQueueConnection("", token());
            queueConnection.start();
            this.session = queueConnection.createQueueSession(false, Session.DUPS_OK_ACKNOWLEDGE);
            Queue queue = session.createQueue(queueName());
            sender = session.createSender(queue);
            sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }

    }

//    public void sendQueueMessage(String jsonAsset) {
//        try {
//            queueConnection = getQueueConnectionFactory().createQueueConnection("", token());
//            try {
//                queueConnection.start();
//                QueueSession session = queueConnection.createQueueSession(false, Session.DUPS_OK_ACKNOWLEDGE);
//                Queue queue = session.createQueue(queueName());
//
//                QueueSender sender = session.createSender(queue);
//
//                sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
//                sender.send(textMessage(session, jsonAsset));
//                sender.close();
//                session.close();
//            } finally {
//                producerConn.stop();
//            }
//        } catch (JMSException e) {
//            throw new RuntimeException("An error occurred when trying to connect to the queue.", e);
//        }
//    }

    private QueueConnectionFactory getQueueConnectionFactory(){
        return (QueueConnectionFactory)getConnectionFactory();
    }

    private ConnectionFactory getConnectionFactory() {
        RMQConnectionFactory rmqCF = new RMQConnectionFactory() {
            private static final long serialVersionUID = 1L;
            @Override
            public Connection createConnection(String userName, String password) throws JMSException {
                if (!isSecure()) {
                    this.setPort(RABBIT_PORT);
                } else {
                    this.setPort(RABBIT_TLS_PORT);
                }
                return super.createConnection(userName, password);
            }
        };
        if (isSecure()) {
            try {
                rmqCF.useSslProtocol();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("An error occurred when trying to use SSL protocol on the RMQConnectionFactory.", e);
            }
        }
        rmqCF.setHost(hostname());
        rmqCF.setQueueBrowserReadMax(qbrMax);
        return rmqCF;
    }

    public void sendMessage(String jsonAsset) {
        try {
            LOGGER.info("Sending asset {}", jsonAsset);
            sender.send(textMessage(this.session, jsonAsset));
        } catch (Exception e) {
            LOGGER.error("Could not send asset {}", jsonAsset);
            throw new RuntimeException(e);
        }
    }

    private Message textMessage(QueueSession session, String message) {
        try {
            return session.createTextMessage(message);
        } catch (JMSException e) {
            throw new RuntimeException("An error occurred when trying to turn the message into a Message object.", e);
        }
    }

    @Override
    protected void shutDown() {
        LOGGER.info("Shutting down {}", this.getClass().getSimpleName());
        try {
            if (this.sender != null) {
                this.sender.close();
            }
            if (this.queueConnection != null) {
                this.queueConnection.stop();
                this.session.close();
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("{} is shut down", this.getClass().getSimpleName());
    }
}
