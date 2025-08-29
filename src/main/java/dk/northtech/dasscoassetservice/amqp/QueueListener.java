package dk.northtech.dasscoassetservice.amqp;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.rabbitmq.jms.admin.RMQConnectionFactory;
import dk.northtech.dasscoassetservice.services.KeycloakService;
import jakarta.jms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public abstract class QueueListener extends AbstractExecutionThreadService {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueListener.class);
    private static final int RABBIT_PORT = 5672;
    private static final int RABBIT_TLS_PORT = 5671;
    private int qbrMax;
    private KeycloakService keycloakService;
    private AMQPConfig amqpConfig;
    String queueName;
    QueueConnection connection;
    QueueSession session;
    Instant lastRestart = null;
    Instant lastError = null;

    public QueueListener(KeycloakService keycloakService, AMQPConfig amqpConfig, String queueName) {
        this.keycloakService = keycloakService;
        this.amqpConfig = amqpConfig;
        this.queueName = queueName;
        this.qbrMax = 0;
    }

    private String queueName() {
        return this.queueName;
    }

    String token() {
        return this.keycloakService.getQueueToken().accessToken();
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
        initSession();
    }

    private QueueConnectionFactory getQueueConnectionFactory() {
        return (QueueConnectionFactory) getConnectionFactory();
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
    protected void run() throws JMSException {
        LOGGER.info("In run method");
        Queue queue = session.createQueue(queueName());
        MessageConsumer messageConsumer = session.createConsumer(queue);

        while (isRunning()) {
            if (lastRestart.plus(58, ChronoUnit.MINUTES).isBefore(Instant.now())) {
                try {
                    if (lastError == null || lastError.plus(1, ChronoUnit.MINUTES).isBefore(Instant.now())) {
                        lastError = null;
                        LOGGER.info("Restarting queue {}", queueName());
                        closeSession();
                        initSession();
                        queue = session.createQueue(queueName());
                        messageConsumer = session.createConsumer(queue);
                        lastRestart = Instant.now();
                    } else {
                        Thread.sleep(5000);
                    }
                } catch (Exception e) {
                    lastError = Instant.now();
                    LOGGER.warn("Failed restart queue");
                }
            } else {
                try {
                    Message message = messageConsumer.receive(2000L);
                    if (message != null) {
                        LOGGER.info("Received a message on {}", this.getClass().getSimpleName());
                    }
                    if (message instanceof TextMessage) {
                        handleMessage(((TextMessage) message).getText());
                    }
                } catch (Exception e) {
                    LOGGER.error("Error while receiving messages", e);
                }
            }
        }
    }

    public abstract void handleMessage(String message);

    @Override
    protected void shutDown() {
        LOGGER.info("Shutting down {}", this.getClass().getSimpleName());
        closeSession();
    }

    public void initSession() {
        try {
            QueueConnection queueConnection = getQueueConnectionFactory().createQueueConnection("", token());
            queueConnection.setExceptionListener(new MyExceptionListener());
            queueConnection.start();
            connection = queueConnection;
            session = queueConnection.createQueueSession(false, Session.DUPS_OK_ACKNOWLEDGE);
            lastRestart = Instant.now();
        } catch (JMSException e) {
            throw new RuntimeException("QueueListener failed to setup the connection", e);
        }
    }

    public void closeSession() {
        try {
            if (session != null) {
                session.close();
                session = null;
            }
            if (connection != null) {
                connection.close();
                connection = null;
            }
        } catch (JMSException e) {
            throw new RuntimeException("An error occurred when trying to shut down " + this.getClass().getSimpleName(), e);
        }
    }

    private static class MyExceptionListener implements ExceptionListener {
        private Logger logger = LoggerFactory.getLogger(MyExceptionListener.class);

        @Override
        public void onException(JMSException exception) {
            logger.info("Connection ExceptionListener fired, exiting");
            logger.warn(exception.getMessage());
            throw new RuntimeException(exception);
        }
    }
}
