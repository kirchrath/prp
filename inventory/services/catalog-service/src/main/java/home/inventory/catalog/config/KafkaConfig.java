package home.inventory.catalog.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    /** Save events are kept forever so other services can rebuild their copies by replaying. */
    @Bean
    NewTopic categoryTopic() {
        return TopicBuilder.name(Topics.CATEGORY).partitions(1).replicas(1).config("retention.ms", "-1").build();
    }

    @Bean
    NewTopic articleTopic() {
        return TopicBuilder.name(Topics.ARTICLE).partitions(1).replicas(1).config("retention.ms", "-1").build();
    }
}
