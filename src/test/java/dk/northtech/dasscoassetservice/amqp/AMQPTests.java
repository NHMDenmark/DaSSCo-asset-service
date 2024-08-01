//package dk.northtech.dasscoassetservice.amqp;
//
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.containers.GenericContainer;
//import org.testcontainers.containers.wait.strategy.Wait;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import org.testcontainers.utility.DockerImageName;
//import jakarta.inject.Inject;
//
//@SpringBootTest
//@Testcontainers
//public class AMQPTests {
//    @Container
//    public static GenericContainer<?> rabbitMQ = new GenericContainer<>(DockerImageName.parse("registry.admin.app.syndev.vd.dk/esdh/amqp-v1.0-rabbitmq:1.0.1"))
//            .withExposedPorts(5672)
//            .waitingFor(Wait.forLogMessage(".*completed with 5 plugins.*", 1));
//
//    @Container
//    static GenericContainer<?> postgisSQL = new GenericContainer(DockerImageName.parse("postgis/postgis:14-3.3"))
//            .withExposedPorts(5432)
//            .withEnv("POSTGRES_DB", "ESDH_kortloesning")
//            .withEnv("POSTGRES_USER", "postgres")
//            .withEnv("POSTGRES_PASSWORD", "test");
//
//    @DynamicPropertySource
//    static void dataSourceProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.liquibase.contexts", () -> "default,  development, test");
//        registry.add("datasource.jdbcUrl", () -> "jdbc:postgresql://localhost:" + postgisSQL.getFirstMappedPort() + "/dassco_file_proxy");
//    }
//
//    @Inject
//    AMQPConfig amqpConfig;
//
//    @BeforeAll
//    static void setUpBeforeClass() {
//        Assertions.assertNotNull(rabbitMQ);
//        System.out.println("Starting RabbitMQ.");
//        rabbitMQ.start();
//        System.out.println("RabbitMQ Started: Now running tests");
//        System.setProperty(
//                "amqp-config.host",
//                "amqp://localhost:" + rabbitMQ.getFirstMappedPort()
//        );
//        System.setProperty(
//                "amqp-config.environment",
//                "unit-testing"
//        );
//    }
//
//    @Test
//    void sendMessage() {
//        AMQPHandler amqpHandler = new AMQPHandler(amqpConfig, AMQPHandler.Command.pub);
//        amqpHandler.run();
//    }
//
//    @Test
//    void subscribeToHandler() {
//        AMQPHandler amqpHandler = new AMQPHandler(amqpConfig, AMQPHandler.Command.sub);
//        amqpHandler.run();
//    }
//}
