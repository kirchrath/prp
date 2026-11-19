package home.inventory.catalog.config;

import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class LookupClientConfig {

    @Bean
    RestClient lookupRestClient(RestClient.Builder builder, LookupProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.timeout());
        return builder.requestFactory(factory)
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .build();
    }
}
