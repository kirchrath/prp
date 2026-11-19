package home.inventory.catalog.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("catalog.lookup")
public record LookupProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue({"https://world.openfoodfacts.org", "https://world.openproductsfacts.org",
                "https://world.openbeautyfacts.org"}) List<String> sources,
        @DefaultValue("4s") Duration timeout,
        @DefaultValue("HomeInventory/0.1 (self-hosted)") String userAgent) {
}
