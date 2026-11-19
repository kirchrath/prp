package home.inventory.notification.config;

import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param ntfyUrl     base URL of the ntfy server (inside Docker: {@code http://ntfy})
 * @param ntfyTopic   topic the household subscribes to
 * @param ntfyToken   access token of the service user (write access to the topic); empty = anonymous
 * @param appUrl      public URL of the inventory app, opened when the notification is tapped
 * @param retryAfter  wait time before a failed daily digest is sent again
 * @param maxAttempts attempts per day for the daily digest
 */
@ConfigurationProperties("inventory.notification")
public record NotificationProperties(String ntfyUrl, String ntfyTopic, @Nullable String ntfyToken, String appUrl,
                                     Duration retryAfter, int maxAttempts) {
}
