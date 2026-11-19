package home.inventory.recipe.config;

import java.net.http.HttpClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AiConfig {

    /** The chat model of the provider selected with {@code spring.ai.model.chat} (ollama, anthropic, openai). */
    @Bean
    ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.create(chatModel);
    }

    /** Client for loading recipe pages. */
    @Bean
    RestClient pageRestClient(RestClient.Builder builder, RecipeProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.fetchTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.fetchTimeout());
        return builder.requestFactory(factory)
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .defaultHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml")
                .build();
    }
}
