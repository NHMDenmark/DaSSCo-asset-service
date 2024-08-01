package dk.northtech.dasscoassetservice.amqp;

import com.rabbitmq.jms.admin.RMQConnectionFactory;
import jakarta.jms.*;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Semaphore;

public class AMQPHandler {
    private static final int RABBIT_PORT = 5672;
    private static final int RABBIT_TLS_PORT = 5671;
    private final Command command;
    private final AMQPConfig amqpConfig;
    private String message = "hello world";

    enum Command { pub, sub }

    public AMQPHandler(AMQPConfig amqpConfig, Command command) {
        this.command = command;
        this.amqpConfig = amqpConfig;
    }

    private String clientSecret() {
        return this.amqpConfig.clientSecret();
    }

    private boolean isSecure() {
        String secureOption = this.amqpConfig.secure();
        return secureOption != null && Boolean.parseBoolean(secureOption);
    }

    private void sendQueueMessage() {
        QueueConnection conn = null;
        try {
            conn = getQueueConnectionFactory().createQueueConnection("", clientSecret());
            try {
                conn.start();
                QueueSession session = conn.createQueueSession(false, Session.DUPS_OK_ACKNOWLEDGE);
                Queue queue = session.createQueue(this.amqpConfig.clientId());

                QueueSender sender = session.createSender(queue);
                sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
                sender.send(textMessage(session));
                System.out.println("Sent message");
                sender.close();
                session.close();
            }finally {
                conn.stop();
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    private Message textMessage(QueueSession session) {
        try {
            return session.createTextMessage(message);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        System.out.printf("Running command %s\n", command);

        switch(command) {
            case pub:
                sendQueueMessage();
                break;
            case sub:
                subscribeQueue();
                break;
        }
    }

    private void subscribeQueue() {
        QueueConnection conn = null;
        try {
            conn = getQueueConnectionFactory().createQueueConnection("", clientSecret());
            try {
                conn.start();
                QueueSession session = conn.createQueueSession(false, Session.DUPS_OK_ACKNOWLEDGE);
                Queue queue = session.createQueue(this.amqpConfig.clientId());
                QueueReceiver receiver = session.createReceiver(queue);

                final Semaphore sem = new Semaphore(0);
                receiver.setMessageListener(message -> {
                    System.out.println("Received message");
                    if (message instanceof TextMessage) {
                        String msgBody = null;
                        try {
                            msgBody = ((TextMessage) message).getText();
                        } catch (JMSException e) {
                            throw new RuntimeException(e);
                        }
                        if (msgBody.equals("exit")) sem.release();
                    }
                });
                sem.acquire();
                session.close();
            } finally {
                conn.stop();
            }
        } catch (JMSException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

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
                throw new RuntimeException(e);
            }
        }
        rmqCF.setHost(this.amqpConfig.host());
        rmqCF.setQueueBrowserReadMax(0); // idk
        return rmqCF;
    }
}
