package home.inventory.stock.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.RetryTopicComponentFactory;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationSupport;
import org.springframework.kafka.retrytopic.RetryTopicNamesProviderFactory;
import org.springframework.kafka.retrytopic.SuffixingRetryTopicNamesProviderFactory.SuffixingRetryTopicNamesProvider;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import tools.jackson.core.JacksonException;

/**
 * Non-blocking retries: a message that fails is moved to the retry topic of this service
 * ({@code <topic>_stock_retry}), the next messages are processed right away. After the last attempt it goes to
 * {@link Topics#DEAD_LETTER} and is logged ({@code DeadLetterHandler}). Unreadable JSON is not retried.
 * Order is not guaranteed any more; every save is protected by versions (local copies ignore older versions,
 * optimistic locking and unique keys turn concurrent saves into errors that are retried).
 */
@Configuration
public class KafkaRetryConfig extends RetryTopicConfigurationSupport {

    @Override
    protected RetryTopicComponentFactory createComponentFactory() {
        return new RetryTopicComponentFactory() {
            @Override
            public RetryTopicNamesProviderFactory retryTopicNamesProviderFactory() {
                // one dead letter topic for all consumed topics of this service
                return properties -> new SuffixingRetryTopicNamesProvider(properties) {
                    @Override
                    public String getTopicName(String topic) {
                        return properties.isDltTopic() ? Topics.DEAD_LETTER : super.getTopicName(topic);
                    }
                };
            }
        };
    }

    /** Resumes the retry topic partitions when the back-off has passed. */
    @Bean
    TaskScheduler retryTopicTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("kafka-retry-");
        return scheduler;
    }

    @Bean
    RetryTopicConfiguration retryTopicConfiguration(KafkaTemplate<String, String> template,
            @Value("${inventory.kafka.retry.attempts:4}") int attempts,
            @Value("${inventory.kafka.retry.backoff:10s}") Duration backoff) {
        return RetryTopicConfigurationBuilder.newInstance()
                .maxAttempts(attempts)
                .fixedBackOff(backoff.toMillis())
                .useSingleTopicForSameIntervals()
                .retryTopicSuffix(Topics.RETRY_SUFFIX)
                .notRetryOn(JacksonException.class)
                .doNotAutoCreateRetryTopics()
                .dltHandlerMethod("deadLetterHandler", "handle")
                .create(template);
    }
}
