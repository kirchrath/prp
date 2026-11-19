package home.inventory.shopping.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    NewTopic itemTopic() {
        return TopicBuilder.name(Topics.ITEM).partitions(1).replicas(1).config("retention.ms", "-1").build();
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
    NewTopic recipeshoppingrequestRetryTopic() {
        return TopicBuilder.name(Topics.RECIPE_SHOPPING_REQUEST + Topics.RETRY_SUFFIX).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic deadLetterTopic() {
        return TopicBuilder.name(Topics.DEAD_LETTER).partitions(1).replicas(1).config("retention.ms", "-1").build();
    }
}
