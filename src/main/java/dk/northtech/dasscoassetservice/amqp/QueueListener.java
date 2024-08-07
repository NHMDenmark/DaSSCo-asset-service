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
import java.util.concurrent.Semaphore;

import static java.time.temporal.ChronoUnit.MINUTES;

@Service
public class QueueListener extends AbstractExecutionThreadService {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueListener.class);
    private static final int RABBIT_PORT = 5672;
    private static final int RABBIT_TLS_PORT = 5671;
    private int qbrMax;
    private KeycloakService keycloakService;
    private AMQPConfig amqpConfig;
    QueueReceiver receiver;
    QueueSession session;
    QueueConnection queueConnection;

    @Inject
    public QueueListener(KeycloakService keycloakService, AMQPConfig amqpConfig) {
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
            queueConnection = getQueueConnectionFactory().createQueueConnection("", token());
            queueConnection.start();
            session = queueConnection.createQueueSession(false, Session.DUPS_OK_ACKNOWLEDGE);
            Queue queue = session.createQueue(queueName());
            this.receiver = session.createReceiver(queue);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }

    }

//    public void subscribeQueue() {
//        QueueConnection conn = null;
//        try {
//            conn = getQueueConnectionFactory().createQueueConnection("", token());
//            try {
//                conn.start();
//                QueueSession session = conn.createQueueSession(false, Session.DUPS_OK_ACKNOWLEDGE);
//                Queue queue = session.createQueue(queueName());
//                QueueReceiver receiver = session.createReceiver(queue);
//
//                final Semaphore sem = new Semaphore(0);
//                receiver.setMessageListener(message -> {
//                    if (message instanceof TextMessage) {
//                        try {
//                            String msgBody = ((TextMessage) message).getText();
//                            System.out.println(msgBody);
//                            if (msgBody.contains("exit")) sem.release();
//                        } catch (JMSException e) {
//                            throw new RuntimeException("An error occurred when trying to read the received message.", e);
//                        }
//                    }
//                });
//                sem.acquire();
//                session.close();
//            } finally {
//                conn.stop();
//            }
//        } catch (JMSException | InterruptedException e) {
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

    @Override
    protected void run() {
        try {
            while (isRunning()) {
//                final Semaphore sem = new Semaphore(0);
                Message message = this.receiver.receive(Duration.of(1, MINUTES).toMillis());
                if (message instanceof TextMessage) {
                    handleMessage(((TextMessage) message).getText());
//                    sem.acquire();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while receiving messages", e);
        }
    }

    public void handleMessage(String message) {
        System.out.println("MESSAGE IN LISTENER:");
        System.out.println(message);
    }

    @Override
    protected void shutDown() {
        LOGGER.info("Shutting down {}", this.getClass().getSimpleName());
        try {
            if (this.receiver != null) {
                this.receiver.close();
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
