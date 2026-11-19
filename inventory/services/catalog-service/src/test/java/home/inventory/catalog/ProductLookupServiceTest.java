package home.inventory.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import home.inventory.catalog.article.ArticleSource;
import home.inventory.catalog.article.ProductInfo;
import home.inventory.catalog.article.ProductLookupService;
import home.inventory.catalog.config.LookupProperties;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ProductLookupServiceTest {

    private static final String FOOD = "https://food.test";
    private static final String PRODUCTS = "https://openproductsfacts.test";
    private static final String BEAUTY = "https://openbeautyfacts.test";

    private MockRestServiceServer server;

    private ProductLookupService service(boolean enabled) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        return new ProductLookupService(
                new LookupProperties(enabled, List.of(FOOD, PRODUCTS, BEAUTY), Duration.ofSeconds(1), "test"),
                builder.build());
    }

    private void respond(String base, String body) {
        server.expect(requestTo(Matchers.startsWith(base + "/api/v2/product/4006381333931")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    @Test
    void disabledOrInvalidBarcodeDoesNotCallAnything() {
        assertThat(service(false).lookup("4006381333931")).isEmpty();
        assertThat(service(true).lookup("abc")).isEmpty();
        server.verify();
    }

    @Test
    void firstSourceWithDataWins() {
        ProductLookupService service = service(true);
        respond(FOOD, "{\"status\":0}");
        respond(PRODUCTS, """
                {"status":1,"product":{"product_name":"Spülmittel","brands":" Pril , Henkel","quantity":"500 ml",
                 "image_front_url":"https://img/x.jpg","unknown":1}}""");
        Optional<ProductInfo> info = service.lookup("4006381333931");
        assertThat(info).contains(new ProductInfo("Spülmittel", "Pril", "500 ml", "https://img/x.jpg",
                ArticleSource.OPEN_PRODUCTS_FACTS));
        server.verify();
    }

    @Test
    void germanNameAndSmallImagePreferred() {
        ProductLookupService service = service(true);
        respond(FOOD, """
                {"status":1,"product":{"product_name":"Pasta","product_name_de":"Nudeln",
                 "image_front_small_url":"small","image_front_url":"big"}}""");
        assertThat(service.lookup("4006381333931"))
                .contains(new ProductInfo("Nudeln", null, null, "small", ArticleSource.OPEN_FOOD_FACTS));
    }

    @Test
    void skipsUnusableAnswersAndErrors() {
        ProductLookupService service = service(true);
        server.expect(requestTo(Matchers.startsWith(FOOD))).andRespond(withServerError());
        respond(PRODUCTS, "{\"status\":1}");
        respond(BEAUTY, "{\"status\":1,\"product\":{\"product_name\":\" \",\"brands\":\"\"}}");
        assertThat(service.lookup("4006381333931")).isEmpty();
        server.verify();
    }

    @Test
    void blankBrandIsIgnored() {
        ProductLookupService service = service(true);
        respond(FOOD, "{\"status\":1,\"product\":{\"product_name\":\"Salz\",\"brands\":\" \"}}");
        assertThat(service.lookup("4006381333931"))
                .contains(new ProductInfo("Salz", null, null, null, ArticleSource.OPEN_FOOD_FACTS));
    }

    @Test
    void emptyBodyIsIgnored() {
        ProductLookupService service = service(true);
        server.expect(requestTo(Matchers.startsWith(FOOD))).andRespond(withSuccess());
        respond(PRODUCTS, "{\"status\":1,\"product\":{\"product_name\":\"X\"}}");
        respond(BEAUTY, "{}");
        assertThat(service.lookup("4006381333931")).map(ProductInfo::source).contains(ArticleSource.OPEN_PRODUCTS_FACTS);
    }

    @Test
    void beautySource() {
        ProductLookupService service = service(true);
        respond(FOOD, "{\"status\":0}");
        respond(PRODUCTS, "{\"status\":0}");
        respond(BEAUTY, "{\"status\":1,\"product\":{\"product_name\":\"Seife\"}}");
        assertThat(service.lookup("4006381333931")).map(ProductInfo::source).contains(ArticleSource.OPEN_BEAUTY_FACTS);
    }
}
