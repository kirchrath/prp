package home.inventory.notification.config;

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
    NewTopic settingsTopic() {
        return forever(Topics.SETTINGS);
    }

    @Bean
    NewTopic digestTopic() {
        return forever(Topics.DIGEST);
    }

    @Bean
    NewTopic catalogarticleRetryTopic() {
        return TopicBuilder.name(Topics.CATALOG_ARTICLE + Topics.RETRY_SUFFIX).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic stockbatchRetryTopic() {
        return TopicBuilder.name(Topics.STOCK_BATCH + Topics.RETRY_SUFFIX).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic deadLetterTopic() {
        return TopicBuilder.name(Topics.DEAD_LETTER).partitions(1).replicas(1).config("retention.ms", "-1").build();
    }
}
