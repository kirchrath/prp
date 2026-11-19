package home.inventory.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class StockIntegrationTest extends AbstractIntegrationTest {

    private String batchId(UUID article, int index) throws Exception {
        return read("/api/stock/batches?articleId=" + article, "$[" + index + "].id");
    }

    @Test
    void stockOutConsumesEarliestBestBeforeFirst() throws Exception {
        UUID location = location(unique("Speisekammer"), null);
        UUID article = article(unique("Nudeln"), ean());
        book("in", article, location, 2, "2027-05-01");
        book("in", article, location, 1, "2027-01-01");
        book("in", article, location, 1, null);
        book("in", article, location, 1, null);

        // partial: stops before the last batches
        mvc.perform(json(post("/api/stock/out"), "{\"articleId\":\"%s\",\"locationId\":\"%s\",\"quantity\":2}".formatted(article, location)))
                .andExpect(jsonPath("$.quantity").value(3));
        mvc.perform(get("/api/stock/batches").param("articleId", article.toString()).with(USER))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].bestBefore").value("2027-05-01"))
                .andExpect(jsonPath("$[0].quantity").value(1))
                .andExpect(jsonPath("$[1].bestBefore").doesNotExist())
                .andExpect(jsonPath("$[1].quantity").value(2));

        // removal of a specific date
        mvc.perform(json(post("/api/stock/out"), "{\"articleId\":\"%s\",\"locationId\":\"%s\",\"quantity\":2,\"bestBefore\":\"2027-05-01\"}"
                        .formatted(article, location)))
                .andExpect(status().isConflict());
        // everything: all batches consumed exactly
        mvc.perform(json(post("/api/stock/out"), "{\"articleId\":\"%s\",\"locationId\":\"%s\",\"quantity\":3}".formatted(article, location)))
                .andExpect(jsonPath("$.quantity").value(0));
        mvc.perform(get("/api/stock/batches").param("articleId", article.toString()).with(USER))
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(get("/api/stock/movements").param("articleId", article.toString()).param("size", "500").with(USER))
                .andExpect(jsonPath("$.totalElements").value(8))
                .andExpect(jsonPath("$.size").value(200))
                .andExpect(jsonPath("$.items[0].delta").value(Matchers.lessThan(0)))
                .andExpect(jsonPath("$.items[0].username").value("alice"))
                .andExpect(jsonPath("$.items[0].source").value("UI"))
                .andExpect(jsonPath("$.items[0].articleName").value(Matchers.startsWith("Nudeln")))
                .andExpect(jsonPath("$.items[0].locationPath").value(Matchers.startsWith("Speisekammer")));
        mvc.perform(get("/api/stock/movements").param("locationId", location.toString()).param("page", "-1").param("size", "0").with(USER))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalPages").value(8));
        mvc.perform(get("/api/stock/movements").with(USER)).andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void bookingNeedsActiveLocation() throws Exception {
        UUID article = article("X", ean());
        mvc.perform(json(post("/api/stock/in"), "{\"articleId\":\"%s\",\"locationId\":\"%s\",\"quantity\":1}".formatted(article, UUID.randomUUID())))
                .andExpect(status().isNotFound());
        mvc.perform(json(post("/api/stock/out"), "{\"articleId\":\"%s\",\"locationId\":\"%s\",\"quantity\":1}".formatted(article, UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void batchesFilteredBySubtreeAndShowUnknownArticles() throws Exception {
        UUID cellar = location(unique("Keller"), null);
        UUID shelf = location("Regal", cellar);
        UUID other = location(unique("Garage"), null);
        UUID known = article("Apfelsaft", ean());
        UUID unknown = UUID.randomUUID();
        book("in", known, shelf, 2, null);
        book("in", unknown, cellar, 1, null);
        book("in", known, other, 1, null);

        mvc.perform(get("/api/stock/batches").param("locationId", cellar.toString()).with(USER))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].articleName").value("Apfelsaft"))
                .andExpect(jsonPath("$[0].articleBrand").value("Marke"))
                .andExpect(jsonPath("$[0].locationPath").value(Matchers.endsWith(" / Regal")))
                .andExpect(jsonPath("$[1].articleName").value("Unbekannter Artikel"))
                .andExpect(jsonPath("$[1].articleBrand").doesNotExist());
        mvc.perform(get("/api/stock/batches").with(USER)).andExpect(jsonPath("$.length()").value(Matchers.greaterThanOrEqualTo(3)));
        mvc.perform(get("/api/stock/movements").param("articleId", unknown.toString()).with(USER))
                .andExpect(jsonPath("$.items[0].articleName").value("Unbekannter Artikel"));

        List<Integer> quantities = com.jayway.jsonpath.JsonPath.read(body("/api/stock/levels"),
                "$[?(@.articleId=='" + known + "')].quantity");
        assertThat(quantities).containsExactly(3);
    }

    @Test
    void correctAndAssignBestBefore() throws Exception {
        UUID location = location(unique("Kühlschrank"), null);
        UUID article = article("Joghurt", ean());
        book("in", article, location, 3, null);
        String batch = batchId(article, 0);

        mvc.perform(get("/api/stock/batches/" + batch).with(USER))
                .andExpect(jsonPath("$.data.quantity").value(3))
                .andExpect(jsonPath("$.audit.version").value(0));

        String target = "2027-02-03";
        mvc.perform(json(post("/api/stock/batches/" + batch + "/best-before"), "{\"audit\":%s,\"quantity\":4,\"bestBefore\":\"%s\"}".formatted(audit(0), target)))
                .andExpect(status().isConflict());
        mvc.perform(json(post("/api/stock/batches/" + batch + "/best-before"), "{\"audit\":%s,\"quantity\":1}".formatted(audit(0))))
                .andExpect(status().isNoContent()); // same date: nothing to do
        mvc.perform(json(post("/api/stock/batches/" + batch + "/best-before"), "{\"audit\":%s,\"quantity\":2,\"bestBefore\":\"%s\"}".formatted(audit(1), target)))
                .andExpect(status().isConflict()); // stale version
        mvc.perform(json(post("/api/stock/batches/" + batch + "/best-before"), "{\"audit\":%s,\"quantity\":2,\"bestBefore\":\"%s\"}".formatted(audit(0), target)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/stock/batches").param("articleId", article.toString()).with(USER))
                .andExpect(jsonPath("$[0].bestBefore").value(target))
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[1].quantity").value(1));

        // correction: unchanged, changed, used up
        mvc.perform(json(put("/api/stock/batches/" + batch + "/quantity"), "{\"audit\":%s,\"quantity\":1}".formatted(audit(1))))
                .andExpect(status().isNoContent());
        mvc.perform(json(put("/api/stock/batches/" + batch + "/quantity"), "{\"audit\":%s,\"quantity\":5}".formatted(audit(1))))
                .andExpect(status().isNoContent());
        mvc.perform(json(put("/api/stock/batches/" + batch + "/quantity"), "{\"audit\":%s,\"quantity\":0}".formatted(audit(2))))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/stock/batches/" + batch).with(USER)).andExpect(status().isNotFound());
        mvc.perform(get("/api/stock/movements").param("articleId", article.toString()).with(USER))
                .andExpect(jsonPath("$.items[0].type").value("CORRECTION"))
                .andExpect(jsonPath("$.items[0].delta").value(-5));

        assertThat(kafka.await("stock_batch", batch, 1).getLast().get("data").get("articleId").asString())
                .isEqualTo(article.toString());
    }

    @Test
    void summaryListsExpiringBatches() throws Exception {
        UUID location = location(unique("Vorrat"), null);
        UUID article = article(unique("Milch"), ean());
        book("in", article, location, 1, LocalDate.now().minusDays(1).toString());
        book("in", article, location, 1, LocalDate.now().plusDays(3).toString());
        book("in", article, location, 1, LocalDate.now().plusDays(30).toString());
        book("in", article, location, 1, null);

        mvc.perform(get("/api/stock/summary").param("days", "7").with(USER))
                .andExpect(jsonPath("$.totalUnits").value(Matchers.greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.expiringSoon[?(@.articleId=='" + article + "')]").value(Matchers.hasSize(2)));
    }
}
