package home.inventory.device.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("device.mqtt")
public record MqttProperties(
        @DefaultValue("tcp://localhost:1883") String url,
        @DefaultValue("device-service") String clientId,
        @DefaultValue("") String username,
        @DefaultValue("") String password,
        @DefaultValue("inventory/scanner") String topicPrefix) {
}
