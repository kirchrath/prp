package home.inventory.article;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import home.inventory.config.InventoryProperties;
import java.net.http.HttpClient;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Looks up barcodes in the Open Food Facts family of databases
 * (food, other products, cosmetics/cleaning). The first hit wins.
 */
@Service
public class ProductLookupService {

    private static final Logger log = LoggerFactory.getLogger(ProductLookupService.class);

    private final InventoryProperties.Lookup properties;
    private final RestClient client;

    ProductLookupService(InventoryProperties properties, RestClient.Builder builder) {
        this.properties = properties.lookup();
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(this.properties.timeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(this.properties.timeout());
        this.client = builder
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.USER_AGENT, this.properties.userAgent())
                .build();
    }

    public Optional<ProductInfo> lookup(String ean) {
        if (!properties.enabled() || !ean.matches("\\d{6,14}")) {
            return Optional.empty();
        }
        for (String base : properties.sources()) {
            try {
                OffResponse response = client.get()
                        .uri(base + "/api/v2/product/{ean}?fields=product_name,product_name_de,brands,quantity,image_front_small_url,image_front_url",
                                ean)
                        .retrieve()
                        .body(OffResponse.class);
                if (response != null && response.status() == 1 && response.product() != null) {
                    Optional<ProductInfo> info = response.product().toInfo(ean, sourceOf(base));
                    if (info.isPresent()) {
                        return info;
                    }
                }
            } catch (RestClientException e) {
                log.debug("Lookup of {} at {} failed: {}", ean, base, e.getMessage());
            }
        }
        return Optional.empty();
    }

    private static ArticleSource sourceOf(String base) {
        if (base.contains("openproductsfacts")) {
            return ArticleSource.OPEN_PRODUCTS_FACTS;
        }
        if (base.contains("openbeautyfacts")) {
            return ArticleSource.OPEN_BEAUTY_FACTS;
        }
        return ArticleSource.OPEN_FOOD_FACTS;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OffResponse(int status, @Nullable OffProduct product) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OffProduct(@JsonProperty("product_name") @Nullable String productName,
                      @JsonProperty("product_name_de") @Nullable String productNameDe,
                      @Nullable String brands,
                      @Nullable String quantity,
                      @JsonProperty("image_front_small_url") @Nullable String imageSmall,
                      @JsonProperty("image_front_url") @Nullable String image) {

        Optional<ProductInfo> toInfo(String ean, ArticleSource source) {
            String name = firstNonBlank(productNameDe, productName);
            if (name == null) {
                return Optional.empty();
            }
            String brand = brands == null ? null : firstNonBlank(brands.split(",")[0]);
            return Optional.of(new ProductInfo(ean, name, brand, firstNonBlank(quantity),
                    firstNonBlank(imageSmall, image), source));
        }

        private static @Nullable String firstNonBlank(@Nullable String... values) {
            for (String v : values) {
                if (v != null && !v.isBlank()) {
                    return v.trim();
                }
            }
            return null;
        }
    }
}
