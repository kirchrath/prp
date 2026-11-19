package home.inventory.catalog.article;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import home.inventory.catalog.config.LookupProperties;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Looks up barcodes in Open Food Facts, Open Products Facts and Open Beauty Facts; the first hit wins. */
@Service
public class ProductLookupService {

    private static final Logger log = LoggerFactory.getLogger(ProductLookupService.class);
    private static final String FIELDS = "product_name,product_name_de,brands,quantity,image_front_small_url,image_front_url";

    private final LookupProperties properties;
    private final RestClient client;

    public ProductLookupService(LookupProperties properties, @Qualifier("lookupRestClient") RestClient client) {
        this.properties = properties;
        this.client = client;
    }

    public Optional<ProductInfo> lookup(String ean) {
        if (!properties.enabled() || !ean.matches("\\d{6,14}")) {
            return Optional.empty();
        }
        for (String base : properties.sources()) {
            Optional<ProductInfo> info = lookupAt(base, ean);
            if (info.isPresent()) {
                return info;
            }
        }
        return Optional.empty();
    }

    private Optional<ProductInfo> lookupAt(String base, String ean) {
        try {
            OffResponse response = client.get()
                    .uri(base + "/api/v2/product/{ean}?fields=" + FIELDS, ean)
                    .retrieve()
                    .body(OffResponse.class);
            if (response == null || response.status() != 1 || response.product() == null) {
                return Optional.empty();
            }
            return response.product().toInfo(sourceOf(base));
        } catch (RestClientException e) {
            log.debug("Lookup of {} at {} failed: {}", ean, base, e.getMessage());
            return Optional.empty();
        }
    }

    static ArticleSource sourceOf(String base) {
        if (base.contains("openproductsfacts")) {
            return ArticleSource.OPEN_PRODUCTS_FACTS;
        }
        if (base.contains("openbeautyfacts")) {
            return ArticleSource.OPEN_BEAUTY_FACTS;
        }
        return ArticleSource.OPEN_FOOD_FACTS;
    }

    static @Nullable String firstNonBlank(@Nullable String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
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

        Optional<ProductInfo> toInfo(ArticleSource source) {
            String name = firstNonBlank(productNameDe, productName);
            if (name == null) {
                return Optional.empty();
            }
            String brand = brands == null ? null : firstNonBlank(brands.split(",")[0]);
            return Optional.of(new ProductInfo(name, brand, firstNonBlank(quantity), firstNonBlank(imageSmall, image),
                    source));
        }
    }
}
