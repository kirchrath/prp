package home.inventory.device.mqtt;

import home.inventory.device.config.MqttConfig;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;

/** Publishes MQTT messages (implemented by Spring Integration). */
@MessagingGateway(defaultRequestChannel = MqttConfig.OUTBOUND_CHANNEL)
public interface MqttPublisher {

    void publish(@Header(MqttHeaders.TOPIC) String topic, String payload);
}
