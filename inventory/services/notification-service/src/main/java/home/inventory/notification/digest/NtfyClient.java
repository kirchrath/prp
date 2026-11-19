package home.inventory.notification.digest;

import home.inventory.notification.config.NotificationProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Publishes messages to the household topic of the ntfy server (JSON publishing, markdown body). */
@Service
public class NtfyClient {

    /** ntfy priorities. */
    public static final int DEFAULT_PRIORITY = 3;
    public static final int HIGH_PRIORITY = 4;

    private final RestClient ntfy;
    private final NotificationProperties properties;

    public NtfyClient(RestClient ntfyRestClient, NotificationProperties properties) {
        this.ntfy = ntfyRestClient;
        this.properties = properties;
    }

    public void send(String title, String message, int priority, List<String> tags) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("topic", properties.ntfyTopic());
        body.put("title", title);
        body.put("message", message);
        body.put("priority", priority);
        body.put("tags", tags);
        body.put("markdown", true);
        body.put("click", properties.appUrl().replaceAll("/+$", "") + "/benachrichtigungen");
        String token = properties.ntfyToken();
        try {
            ntfy.post()
                    .uri("/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> {
                        if (token != null && !token.isBlank()) {
                            h.setBearerAuth(token);
                        }
                    })
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new NotificationFailedException("ntfy: " + e.getMessage(), e);
        }
    }
}
