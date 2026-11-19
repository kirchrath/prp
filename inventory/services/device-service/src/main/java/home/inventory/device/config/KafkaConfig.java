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
    NewTopic firmwareTopic() {
        return TopicBuilder.name(Topics.FIRMWARE).partitions(1).replicas(1).config("retention.ms", "-1").build();
    }

    @Bean
    NewTopic otaJobTopic() {
        return TopicBuilder.name(Topics.OTA_JOB).partitions(1).replicas(1).config("retention.ms", "-1").build();
    }

    @Bean
    NewTopic scanTopic() {
        return TopicBuilder.name(Topics.SCAN).partitions(1).replicas(1).config("retention.ms", "-1").build();
    }

    @Bean
    NewTopic stockbookingRetryTopic() {
        return TopicBuilder.name(Topics.STOCK_BOOKING + Topics.RETRY_SUFFIX).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic deadLetterTopic() {
        return TopicBuilder.name(Topics.DEAD_LETTER).partitions(1).replicas(1).config("retention.ms", "-1").build();
    }
}
