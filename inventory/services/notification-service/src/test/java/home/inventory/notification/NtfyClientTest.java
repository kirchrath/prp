package home.inventory.notification;

import static org.assertj.core.api.Assertions.assertThat;

import home.inventory.notification.config.NotificationProperties;
import home.inventory.notification.digest.NtfyClient;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class NtfyClientTest {

    @BeforeEach
    void reset() {
        FakeNtfy.reset();
    }

    private static NtfyClient client(String token) {
        var properties = new NotificationProperties(FakeNtfy.url(), "kueche", token, "http://app",
                Duration.ofMinutes(15), 4);
        return new NtfyClient(RestClient.create(FakeNtfy.url()), properties);
    }

    @Test
    void sendsWithoutTokenWhenNoneIsConfigured() {
        client(null).send("T", "M", 3, List.of());
        client(" ").send("T", "M", 3, List.of());
        assertThat(FakeNtfy.requests()).extracting(FakeNtfy.Request::authorization).containsOnlyNulls();
        assertThat(FakeNtfy.requests().getFirst().body()).contains("\"click\":\"http://app/benachrichtigungen\"",
                "\"topic\":\"kueche\"");
    }
}
