package home.inventory.device;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:18.7-alpine");
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafka() {
        return new KafkaContainer("apache/kafka:4.2.0");
    }

    @Bean
    GenericContainer<?> mosquitto() {
        return new GenericContainer<>("eclipse-mosquitto:2.1-alpine")
                .withCopyToContainer(Transferable.of("listener 1883\nallow_anonymous true\n"),
                        "/mosquitto/config/mosquitto.conf")
                .withExposedPorts(1883);
    }

    @Bean
    DynamicPropertyRegistrar mqttProperties(GenericContainer<?> mosquitto) {
        return registry -> registry.add("device.mqtt.url",
                () -> "tcp://" + mosquitto.getHost() + ":" + mosquitto.getMappedPort(1883));
    }
}
