package home.inventory.notification.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(NotificationProperties.class)
public class SchedulingConfig {

    /** Time zone of the container (TZ); the send time of the daily digest is local time. */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    RestClient ntfyRestClient(NotificationProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return RestClient.builder().baseUrl(properties.ntfyUrl()).requestFactory(factory).build();
    }
}
