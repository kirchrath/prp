package home.inventory.stock.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    private static NewTopic forever(String name) {
        return TopicBuilder.name(name).partitions(1).replicas(1).config("retention.ms", "-1").build();
    }

    @Bean
    NewTopic locationTopic() {
        return forever(Topics.LOCATION);
    }

    @Bean
    NewTopic batchTopic() {
        return forever(Topics.BATCH);
    }

    @Bean
    NewTopic movementTopic() {
        return forever(Topics.MOVEMENT);
    }

    @Bean
    NewTopic bookingTopic() {
        return forever(Topics.BOOKING);
    }
}
