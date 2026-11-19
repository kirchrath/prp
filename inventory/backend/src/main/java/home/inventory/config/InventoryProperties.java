package home.inventory.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("inventory")
public record InventoryProperties(Mqtt mqtt, Lookup lookup) {

    public record Mqtt(
            @DefaultValue("false") boolean enabled,
            @DefaultValue("tcp://mosquitto:1883") String url,
            @DefaultValue("inventory-backend") String clientId,
            @DefaultValue("inventory-backend") String username,
            @DefaultValue("") String password,
            @DefaultValue("inventory/scanner") String topicPrefix) {
    }

    public record Lookup(
            @DefaultValue("true") boolean enabled,
            @DefaultValue({"https://world.openfoodfacts.org", "https://world.openproductsfacts.org",
                    "https://world.openbeautyfacts.org"}) List<String> sources,
            @DefaultValue("4s") Duration timeout,
            @DefaultValue("HomeInventory/0.1 (self-hosted)") String userAgent) {
    }
}
