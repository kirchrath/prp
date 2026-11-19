package home.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import home.inventory.scanner.ScannerMessages.ScanMessage;
import home.inventory.scanner.ScannerMessages.StatusMessage;
import home.inventory.scanner.ScannerService;
import home.inventory.stock.ScanOutcome;
import home.inventory.stock.ScanResult;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class ScanFlowTest extends AbstractIntegrationTest {

    private static final RequestPostProcessor USER = jwt().jwt(j -> j.subject("alice").claim("preferred_username", "alice"));

    @Autowired
    MockMvc mvc;

    @Autowired
    ScannerService scanners;

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        mvc.perform(get("/api/articles")).andExpect(status().isUnauthorized());
    }

    @Test
    void unknownBarcodeIsStoredInInboxAndCanBeRemovedAgain() throws Exception {
        long location = createLocation("Keller-" + UUID.randomUUID(), null);
        String ean = randomEan();

        mvc.perform(json(post("/api/stock/scan"), "{\"ean\":\"%s\",\"locationId\":%d,\"direction\":\"IN\"}".formatted(ean, location)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("IN_OK_UNKNOWN_ARTICLE"))
                .andExpect(jsonPath("$.quantityAtLocation").value(1));
        mvc.perform(json(post("/api/stock/scan"), "{\"ean\":\"%s\",\"locationId\":%d,\"direction\":\"IN\"}".formatted(ean, location)))
                .andExpect(jsonPath("$.outcome").value("IN_OK"))
                .andExpect(jsonPath("$.quantityAtLocation").value(2));

        mvc.perform(get("/api/articles/by-ean/" + ean).with(USER))
                .andExpect(jsonPath("$.status").value("UNKNOWN"))
                .andExpect(jsonPath("$.totalQuantity").value(2));

        for (int i = 0; i < 2; i++) {
            mvc.perform(json(post("/api/stock/scan"), "{\"ean\":\"%s\",\"locationId\":%d,\"direction\":\"OUT\"}".formatted(ean, location)))
                    .andExpect(jsonPath("$.outcome").value("OUT_OK"));
        }
        mvc.perform(json(post("/api/stock/scan"), "{\"ean\":\"%s\",\"locationId\":%d,\"direction\":\"OUT\"}".formatted(ean, location)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("NOT_IN_STOCK"));

        mvc.perform(get("/api/movements").param("locationId", String.valueOf(location)).with(USER))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.items[0].username").value("alice"))
                .andExpect(jsonPath("$.items[0].source").value("MOBILE"));
    }

    @Test
    void stockOutConsumesEarliestBestBeforeFirst() throws Exception {
        long location = createLocation("Speisekammer-" + UUID.randomUUID(), null);
        long article = createArticle("Nudeln", 0, randomEan());
        book("in", article, location, 2, "2027-05-01");
        book("in", article, location, 1, "2027-01-01");
        book("in", article, location, 1, null);

        book("out", article, location, 2, null).andExpect(jsonPath("$.quantity").value(2));

        mvc.perform(get("/api/stock").param("articleId", String.valueOf(article)).with(USER))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].bestBefore").value("2027-05-01"))
                .andExpect(jsonPath("$[0].quantity").value(1))
                .andExpect(jsonPath("$[1].bestBefore").doesNotExist());
    }

    @Test
    void bestBeforeCanBeAssignedLater() throws Exception {
        long location = createLocation("Kühlschrank-" + UUID.randomUUID(), null);
        long article = createArticle("Joghurt", 0, randomEan());
        book("in", article, location, 3, null);
        String stock = mvc.perform(get("/api/stock").param("articleId", String.valueOf(article)).with(USER))
                .andReturn().getResponse().getContentAsString();
        int batchId = JsonPath.read(stock, "$[0].id");

        mvc.perform(json(post("/api/stock/batches/" + batchId + "/best-before"), "{\"quantity\":2,\"bestBefore\":\"2027-02-03\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(2));
        mvc.perform(get("/api/stock").param("articleId", String.valueOf(article)).with(USER))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].bestBefore").value("2027-02-03"))
                .andExpect(jsonPath("$[1].quantity").value(1));
    }

    @Test
    void shoppingListFollowsMinimumStock() throws Exception {
        long location = createLocation("Bad-" + UUID.randomUUID(), null);
        long article = createArticle("Spülmittel-" + UUID.randomUUID(), 2, randomEan());
        book("in", article, location, 1, null);
        assertThat(shoppingQuantity(article)).contains(1);
        book("in", article, location, 1, null);
        assertThat(shoppingQuantity(article)).isEmpty();
        book("out", article, location, 2, null);
        assertThat(shoppingQuantity(article)).contains(2);
    }

    @Test
    void locationsAreHierarchicalAndStockIsFilteredBySubtree() throws Exception {
        long keller = createLocation("Keller-" + UUID.randomUUID(), null);
        long regal = createLocation("Regal 1", keller);
        long article = createArticle("Reis", 0, randomEan());
        book("in", article, regal, 5, null);

        mvc.perform(get("/api/stock").param("locationId", String.valueOf(keller)).with(USER))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].locationPath").value(org.hamcrest.Matchers.matchesPattern("Keller-.+ / Regal 1")));

        mvc.perform(json(put("/api/locations/" + keller), "{\"name\":\"Keller\",\"parentId\":%d}".formatted(regal)))
                .andExpect(status().isConflict());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/locations/" + regal).with(USER))
                .andExpect(status().isConflict());
    }

    @Test
    void scannerRegistersItselfAndNeedsLocation() throws Exception {
        String device = "scanner-" + UUID.randomUUID().toString().substring(0, 8);
        scanners.handleStatus(device, new StatusMessage(true, "0.1.0", "192.168.1.50", -60, "b1"));
        ScanResult noLocation = scanners.handleScan(device, new ScanMessage(randomEan(), "IN", 1L, "b1")).orElseThrow();
        assertThat(noLocation.outcome()).isEqualTo(ScanOutcome.NO_LOCATION);

        long location = createLocation("Vorrat-" + UUID.randomUUID(), null);
        String list = mvc.perform(get("/api/scanners").with(USER)).andReturn().getResponse().getContentAsString();
        java.util.List<Integer> ids = JsonPath.read(list, "$[?(@.deviceId=='" + device + "')].id");
        int scannerId = ids.getFirst();

        mvc.perform(json(put("/api/scanners/" + scannerId), "{\"name\":\"Vorratsregal\",\"locationId\":%d}".formatted(location)))
                .andExpect(jsonPath("$.online").value(true))
                .andExpect(jsonPath("$.locationId").value(location));

        String ean = randomEan();
        Optional<ScanResult> first = scanners.handleScan(device, new ScanMessage(ean, "in", 2L, "b1"));
        assertThat(first).get().extracting(ScanResult::outcome).isEqualTo(ScanOutcome.IN_OK_UNKNOWN_ARTICLE);
        // QoS-1 redelivery of the same message is ignored
        assertThat(scanners.handleScan(device, new ScanMessage(ean, "IN", 2L, "b1"))).isEmpty();
        // after a reboot the sequence starts again
        assertThat(scanners.handleScan(device, new ScanMessage(ean, "OUT", 1L, "b2")))
                .get().extracting(ScanResult::quantityAtLocation).isEqualTo(0L);
        assertThat(scanners.handleScan(device, new ScanMessage(ean, "sideways", 2L, "b2")))
                .get().extracting(ScanResult::outcome).isEqualTo(ScanOutcome.ERROR);
    }

    @Test
    void inboxArticleCanBeMergedIntoExistingArticle() throws Exception {
        long location = createLocation("Garage-" + UUID.randomUUID(), null);
        long target = createArticle("Wasser", 0, randomEan());
        book("in", target, location, 1, null);
        String unknownEan = randomEan();
        String scan = mvc.perform(json(post("/api/stock/scan"),
                        "{\"ean\":\"%s\",\"locationId\":%d,\"direction\":\"IN\"}".formatted(unknownEan, location)))
                .andReturn().getResponse().getContentAsString();
        int placeholder = JsonPath.read(scan, "$.articleId");

        mvc.perform(post("/api/articles/" + placeholder + "/merge-into/" + target).with(USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(2))
                .andExpect(jsonPath("$.eans.length()").value(2));
        mvc.perform(get("/api/articles/" + placeholder).with(USER)).andExpect(status().isNotFound());
        mvc.perform(get("/api/articles/by-ean/" + unknownEan).with(USER)).andExpect(jsonPath("$.id").value(target));
    }

    // --- helpers ---------------------------------------------------------------------------------------------

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.with(USER).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private long createLocation(String name, Long parent) throws Exception {
        String body = "{\"name\":\"%s\",\"parentId\":%s}".formatted(name, parent);
        String response = mvc.perform(json(post("/api/locations"), body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private long createArticle(String name, int minStock, String ean) throws Exception {
        String body = "{\"name\":\"%s\",\"minStock\":%d,\"eans\":[\"%s\"]}".formatted(name, minStock, ean);
        String response = mvc.perform(json(post("/api/articles"), body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private org.springframework.test.web.servlet.ResultActions book(String dir, long article, long location, int qty,
                                                                      String bestBefore) throws Exception {
        String bb = bestBefore == null ? "null" : "\"" + bestBefore + "\"";
        return mvc.perform(json(post("/api/stock/" + dir),
                        "{\"articleId\":%d,\"locationId\":%d,\"quantity\":%d,\"bestBefore\":%s}".formatted(article, location, qty, bb)))
                .andExpect(status().isOk());
    }

    private Optional<Integer> shoppingQuantity(long article) throws Exception {
        String list = mvc.perform(get("/api/shopping-list").with(USER)).andReturn().getResponse().getContentAsString();
        java.util.List<Integer> q = JsonPath.read(list, "$[?(@.articleId==" + article + " && @.automatic==true)].quantity");
        return q.stream().findFirst();
    }

    private static String randomEan() {
        return String.valueOf(4_000_000_000_000L + (long) (Math.random() * 999_999_999_999L));
    }
}
