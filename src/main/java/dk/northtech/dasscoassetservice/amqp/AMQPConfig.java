package dk.northtech.dasscoassetservice.amqp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "amqp-config")
public record AMQPConfig (String host
        , String assetQueueName
        , String acknowledgeQueueName
//        , String clientId
//        , String clientSecret
//        , String tenantId
//        , int connectionTTL
//        , int messageAgeThreshold
//        , String environment
        , String secure
) {

}