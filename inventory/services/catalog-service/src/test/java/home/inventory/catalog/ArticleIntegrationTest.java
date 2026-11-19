package home.inventory.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import home.inventory.catalog.article.ArticleService;
import home.inventory.catalog.article.ArticleSource;
import home.inventory.catalog.article.ProductInfo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

class ArticleIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ArticleService articles;

    private UUID createCategory(String name) throws Exception {
        return created(json(post("/api/catalog/categories"), "{\"data\":{\"name\":\"%s\"}}".formatted(name)));
    }

    private UUID createArticle(String name, String brand, UUID categoryId, String... eans) throws Exception {
        String eanList = String.join("\",\"", eans);
        String body = """
                {"data":{"name":"%s","brand":%s,"packageSize":" 500 g ","imageUrl":" ","categoryId":%s,"minStock":2,
                 "eans":[%s]}}""".formatted(name, brand == null ? "null" : "\"" + brand + "\"",
                categoryId == null ? "null" : "\"" + categoryId + "\"", eans.length == 0 ? "" : "\"" + eanList + "\"");
        return created(json(post("/api/catalog/articles"), body));
    }

    private static String ean() {
        return String.valueOf(4_000_000_000_000L + (long) (Math.random() * 999_999_999_999L));
    }

    @Test
    void createReadUpdateDelete() throws Exception {
        UUID category = createCategory(unique("Vorrat"));
        String ean = ean();
        String name = unique("Spaghetti");
        UUID id = createArticle(name, "Barilla", category, ean);

        mvc.perform(get("/api/catalog/articles/" + id).with(USER))
                .andExpect(jsonPath("$.data.name").value(name))
                .andExpect(jsonPath("$.data.packageSize").value("500 g"))
                .andExpect(jsonPath("$.data.imageUrl").doesNotExist())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.source").value("MANUAL"))
                .andExpect(jsonPath("$.data.eans[0]").value(ean))
                .andExpect(jsonPath("$.audit.version").value(0));

        mvc.perform(get("/api/catalog/articles/by-ean/" + ean).with(USER))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.categoryName").value(Matchers.startsWith("Vorrat")));

        String ean2 = ean();
        mvc.perform(json(put("/api/catalog/articles/" + id), """
                        {"audit":%s,"data":{"name":"%s","brand":" ","minStock":1,"eans":["%s","%s"]}}"""
                        .formatted(audit(0), name, ean, ean2)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/catalog/articles/by-ean/" + ean2).with(USER))
                .andExpect(jsonPath("$.brand").doesNotExist())
                .andExpect(jsonPath("$.categoryName").doesNotExist());
        mvc.perform(json(put("/api/catalog/articles/" + id), "{\"audit\":" + audit(0) + ",\"data\":{\"name\":\"x\",\"minStock\":0,\"eans\":[]}}"))
                .andExpect(status().isConflict());
        mvc.perform(json(put("/api/catalog/articles/" + id), "{\"data\":{\"name\":\"x\",\"minStock\":0,\"eans\":[]}}"))
                .andExpect(status().isConflict());

        mvc.perform(delete("/api/catalog/articles/" + id).param("version", "1").with(USER)).andExpect(status().isNoContent());
        mvc.perform(get("/api/catalog/articles/by-ean/" + ean).with(USER)).andExpect(status().isNotFound());
        mvc.perform(get("/api/catalog/articles/" + UUID.randomUUID()).with(USER)).andExpect(status().isNotFound());

        List<JsonNode> events = kafka.await("catalog_article", id.toString(), 3);
        assertThat(events.getLast().get("audit").get("deleted").asBoolean()).isTrue();
        assertThat(events.getLast().get("data").get("minStock").asInt()).isEqualTo(1);
    }

    @Test
    void barcodesAreUniqueAndCategoryMustExist() throws Exception {
        String ean = ean();
        createArticle(unique("A"), null, null, ean);
        mvc.perform(json(post("/api/catalog/articles"), "{\"data\":{\"name\":\"B\",\"minStock\":0,\"eans\":[\"%s\"]}}".formatted(ean)))
                .andExpect(status().isConflict());
        mvc.perform(json(post("/api/catalog/articles"), "{\"data\":{\"name\":\"B\",\"minStock\":0,\"eans\":[],\"categoryId\":\"%s\"}}"
                        .formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
        UUID deletedCategory = createCategory(unique("Alt"));
        mvc.perform(delete("/api/catalog/categories/" + deletedCategory).param("version", "0").with(USER))
                .andExpect(status().isNoContent());
        mvc.perform(json(post("/api/catalog/articles"), "{\"data\":{\"name\":\"B\",\"minStock\":0,\"eans\":[],\"categoryId\":\"%s\"}}"
                        .formatted(deletedCategory)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listFiltersByStatusCategoryAndSearch() throws Exception {
        UUID category = createCategory(unique("Bad"));
        String brand = unique("Marke");
        String ean = ean();
        UUID inCategory = createArticle(unique("Zahnpasta"), brand, category, ean);
        UUID other = createArticle(unique("Shampoo"), null, null);
        articles.resolvePendingBarcode(ean() + " ");

        mvc.perform(get("/api/catalog/articles").param("categoryId", category.toString()).with(USER))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(inCategory.toString()))
                .andExpect(jsonPath("$[0].categoryName").value(Matchers.startsWith("Bad")));
        mvc.perform(get("/api/catalog/articles").param("search", brand.toUpperCase()).with(USER))
                .andExpect(jsonPath("$[*].id").value(Matchers.contains(inCategory.toString())));
        mvc.perform(get("/api/catalog/articles").param("search", ean).with(USER))
                .andExpect(jsonPath("$[*].id").value(Matchers.contains(inCategory.toString())));
        mvc.perform(get("/api/catalog/articles").param("search", "shampoo").with(USER))
                .andExpect(jsonPath("$[*].id").value(Matchers.hasItem(other.toString())));
        mvc.perform(get("/api/catalog/articles").param("search", " ").with(USER))
                .andExpect(jsonPath("$[*].id").value(Matchers.hasItems(other.toString(), inCategory.toString())));
        mvc.perform(get("/api/catalog/articles").param("status", "UNKNOWN").with(USER))
                .andExpect(jsonPath("$[*].status").value(Matchers.everyItem(Matchers.is("UNKNOWN"))))
                .andExpect(jsonPath("$[*].name").value(Matchers.hasItem(Matchers.startsWith("Unbekannt ("))));
    }

    @Test
    void lookupReportsProductDataAndExistingArticle() throws Exception {
        String ean = ean();
        UUID id = createArticle(unique("Milch"), null, null, ean);
        when(lookup.lookup(ean)).thenReturn(Optional.of(
                new ProductInfo("H-Milch", "Weihenstephan", "1 l", "img", ArticleSource.OPEN_FOOD_FACTS)));

        mvc.perform(get("/api/catalog/articles/lookup/" + ean).with(USER))
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.name").value("H-Milch"))
                .andExpect(jsonPath("$.existingArticleId").value(id.toString()));
        mvc.perform(get("/api/catalog/articles/lookup/" + ean()).with(USER))
                .andExpect(jsonPath("$.found").value(false))
                .andExpect(jsonPath("$.existingArticleId").doesNotExist());
    }

    @Test
    void pendingBarcodeIsResolvedFromProductDatabaseOrAsPlaceholder() throws Exception {
        String known = ean();
        when(lookup.lookup(known)).thenReturn(Optional.of(
                new ProductInfo("Reis", "Uncle", "1 kg", "img", ArticleSource.OPEN_FOOD_FACTS)));
        articles.resolvePendingBarcode(known);
        mvc.perform(get("/api/catalog/articles/by-ean/" + known).with(USER))
                .andExpect(jsonPath("$.name").value("Reis"))
                .andExpect(jsonPath("$.brand").value("Uncle"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        String unknown = ean();
        articles.resolvePendingBarcode(unknown);
        String body = mvc.perform(get("/api/catalog/articles/by-ean/" + unknown).with(USER))
                .andExpect(jsonPath("$.status").value("UNKNOWN"))
                .andReturn().getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(body, "$.id");

        // an already existing article is published again instead of creating a duplicate
        articles.resolvePendingBarcode(unknown);
        assertThat(kafka.await("catalog_article", id, 2)).hasSize(2);
        mvc.perform(get("/api/catalog/articles").param("search", unknown).with(USER))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void mergeMovesBarcodesToTarget() throws Exception {
        String targetEan = ean();
        UUID target = createArticle(unique("Wasser"), null, null, targetEan);
        String unknownEan = ean();
        articles.resolvePendingBarcode(unknownEan);
        String body = mvc.perform(get("/api/catalog/articles/by-ean/" + unknownEan).with(USER))
                .andReturn().getResponse().getContentAsString();
        UUID source = UUID.fromString(com.jayway.jsonpath.JsonPath.read(body, "$.id"));

        mvc.perform(post("/api/catalog/articles/" + source + "/merge-into/" + source).param("version", "0").with(USER))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/catalog/articles/" + source + "/merge-into/" + target).param("version", "0").with(USER))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/catalog/articles/by-ean/" + unknownEan).with(USER))
                .andExpect(jsonPath("$.id").value(target.toString()));
        mvc.perform(get("/api/catalog/articles/" + target).with(USER))
                .andExpect(jsonPath("$.data.eans.length()").value(2));
        mvc.perform(get("/api/catalog/articles/" + source).with(USER))
                .andExpect(jsonPath("$.audit.deleted").value(true))
                .andExpect(jsonPath("$.data.mergedInto").value(target.toString()))
                .andExpect(jsonPath("$.data.eans.length()").value(0));
        JsonNode sourceEvent = kafka.await("catalog_article", source.toString(), 2).getLast();
        assertThat(sourceEvent.get("data").get("mergedInto").asString()).isEqualTo(target.toString());
    }

    @Test
    void pendingBookingsFromStockCreateArticles() throws Exception {
        String ean = ean();
        UUID booking = UUID.randomUUID();
        String audit = "{\"createdDate\":\"2026-01-01T00:00:00Z\",\"lastModifiedDate\":\"2026-01-01T00:00:00Z\",\"version\":0,\"deleted\":%s}";
        // ignored: deleted, other status, no barcode
        kafka.send("stock_booking", booking.toString(), "{\"id\":\"%s\",\"audit\":%s,\"data\":{\"ean\":\"%s\",\"status\":\"PENDING_ARTICLE\"}}"
                .formatted(booking, audit.formatted(true), ean));
        kafka.send("stock_booking", booking.toString(), "{\"id\":\"%s\",\"audit\":%s,\"data\":{\"ean\":\"%s\",\"status\":\"IN_OK\",\"x\":1}}"
                .formatted(booking, audit.formatted(false), ean));
        kafka.send("stock_booking", booking.toString(), "{\"id\":\"%s\",\"audit\":%s,\"data\":{\"status\":\"PENDING_ARTICLE\"}}"
                .formatted(booking, audit.formatted(false)));
        kafka.send("stock_booking", booking.toString(), "{\"id\":\"%s\",\"audit\":%s,\"data\":{\"ean\":\"%s\",\"status\":\"PENDING_ARTICLE\"}}"
                .formatted(booking, audit.formatted(false), ean));

        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(20)).untilAsserted(() ->
                mvc.perform(get("/api/catalog/articles/by-ean/" + ean).with(USER)).andExpect(status().isOk()));
        mvc.perform(get("/api/catalog/articles").param("search", ean).with(USER))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void lookupMockDefaultsToNothing() {
        when(lookup.lookup(anyString())).thenReturn(Optional.empty());
        assertThat(lookup.lookup("1")).isEmpty();
    }
}
