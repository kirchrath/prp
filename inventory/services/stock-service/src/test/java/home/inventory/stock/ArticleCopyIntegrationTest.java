package home.inventory.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import home.inventory.stock.article.ArticleCopyRepository;
import home.inventory.stock.article.BarcodeCopyRepository;
import java.time.Duration;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ArticleCopyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ArticleCopyRepository copies;

    @Autowired
    BarcodeCopyRepository barcodes;

    @Test
    void keepsNewestStateAndBarcodes() {
        UUID id = UUID.randomUUID();
        String a = ean();
        String b = ean();
        String c = ean();
        article(id, 1, false, "Neu", "ACTIVE", null, a, b);
        article(id, 0, false, "Alt", "ACTIVE", null, c); // older version is ignored
        assertThat(copies.findByArticleId(id)).get().extracting("name").isEqualTo("Neu");

        article(id, 2, false, "Neuer", "ACTIVE", null, b, c);
        assertThat(barcodes.findAllByArticleIdAndDeletedFalse(id)).extracting("ean").containsExactlyInAnyOrder(b, c);

        article(id, 3, true, "Neuer", "ACTIVE", null, b, c);
        assertThat(barcodes.findAllByArticleIdAndDeletedFalse(id)).isEmpty();
        assertThat(copies.findByArticleId(id)).get().extracting("deleted").isEqualTo(true);
    }

    @Test
    void mergedArticleStockMovesToTarget() throws Exception {
        UUID location = location(unique("Keller"), null);
        UUID source = article("Unbekannt", ean());
        UUID target = article("Wasser", ean());
        book("in", source, location, 2, null);
        book("in", source, location, 1, "2027-01-01");
        book("in", target, location, 1, null);

        article(source, 1, true, "Unbekannt", "UNKNOWN", target);

        mvc.perform(get("/api/stock/batches").param("articleId", target.toString()).with(USER))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].quantity").value(1))
                .andExpect(jsonPath("$[1].quantity").value(3));
        mvc.perform(get("/api/stock/batches").param("articleId", source.toString()).with(USER))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/stock/movements").param("articleId", target.toString()).with(USER))
                .andExpect(jsonPath("$.items[0].type").value("MERGE"))
                .andExpect(jsonPath("$.items[0].source").value("SYSTEM"));
    }

    @Test
    void articlesArriveViaKafka() {
        UUID id = UUID.randomUUID();
        kafka.send("catalog_article", id.toString(), """
                {"id":"%s","audit":{"createdDate":"2026-01-01T00:00:00Z","lastModifiedDate":"2026-01-01T00:00:00Z",
                 "version":0,"deleted":false},"data":{"name":"Salz","status":"ACTIVE","eans":["%s"],"minStock":2}}"""
                .formatted(id, ean()));
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> copies.findByArticleId(id).isPresent());
    }
}
