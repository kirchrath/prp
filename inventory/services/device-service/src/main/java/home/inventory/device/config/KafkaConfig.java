package home.inventory.device.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    NewTopic scannerTopic() {
        return TopicBuilder.name(Topics.SCANNER).partitions(1).replicas(1).config("retention.ms", "-1").build();
    }

    @Bean
    NewTopic scanTopic() {
        return TopicBuilder.name(Topics.SCAN).partitions(1).replicas(1).config("retention.ms", "-1").build();
    }
}
