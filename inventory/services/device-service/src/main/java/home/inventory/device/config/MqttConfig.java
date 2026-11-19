package home.inventory.device.config;

import home.inventory.device.mqtt.MqttInboundService;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageHandler;

/**
 * MQTT connection to the scanners (Spring Integration). Connection handling and reconnects are done by the
 * adapters; messages are handed to {@link MqttInboundService} one after another in arrival order.
 */
@Configuration
public class MqttConfig {

    public static final String OUTBOUND_CHANNEL = "mqttOutboundChannel";

    @Bean
    MqttPahoClientFactory mqttClientFactory(MqttProperties properties) {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{properties.url()});
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setKeepAliveInterval(30);
        if (!properties.username().isBlank()) {
            options.setUserName(properties.username());
            options.setPassword(properties.password().toCharArray());
        }
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    IntegrationFlow mqttInboundFlow(MqttPahoClientFactory factory, MqttProperties properties,
                                    MqttInboundService inbound) {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                properties.clientId() + "-in", factory,
                properties.topicPrefix() + "/+/scan", properties.topicPrefix() + "/+/button",
                properties.topicPrefix() + "/+/status");
        adapter.setQos(1);
        return IntegrationFlow.from(adapter)
                .handle(String.class, (payload, headers) -> {
                    inbound.handle((String) headers.get(MqttHeaders.RECEIVED_TOPIC), payload);
                    return null;
                })
                .get();
    }

    @Bean
    @ServiceActivator(inputChannel = OUTBOUND_CHANNEL)
    MessageHandler mqttOutbound(MqttPahoClientFactory factory, MqttProperties properties) {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(properties.clientId() + "-out", factory);
        handler.setAsync(true);
        handler.setDefaultQos(1);
        return handler;
    }
}
