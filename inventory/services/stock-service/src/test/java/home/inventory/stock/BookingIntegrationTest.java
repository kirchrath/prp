package home.inventory.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import home.inventory.stock.booking.BookingRepository;
import home.inventory.stock.booking.BookingService;
import home.inventory.stock.booking.dto.ScanDataDto;
import home.inventory.stock.booking.dto.ScanEventDto;
import home.inventory.stock.common.dto.AuditDto;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

class BookingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    BookingService bookings;

    @Autowired
    BookingRepository repository;

    private UUID scan(String ean, UUID location, String direction) throws Exception {
        return created(json(post("/api/stock/bookings"),
                "{\"ean\":\" %s \",\"locationId\":\"%s\",\"direction\":\"%s\"}".formatted(ean, location, direction)));
    }

    private String statusOf(UUID booking) throws Exception {
        return read("/api/stock/bookings/" + booking, "$.data.status");
    }

    @Test
    void knownArticleInAndOut() throws Exception {
        UUID location = location(unique("Schrank"), null);
        String ean = ean();
        UUID article = article("Kaffee", ean);

        UUID in = scan(ean, location, "IN");
        mvc.perform(get("/api/stock/bookings/" + in).with(USER))
                .andExpect(jsonPath("$.data.status").value("IN_OK"))
                .andExpect(jsonPath("$.data.articleId").value(article.toString()))
                .andExpect(jsonPath("$.data.quantityAtLocation").value(1))
                .andExpect(jsonPath("$.data.source").value("MOBILE"))
                .andExpect(jsonPath("$.data.username").value("alice"));
        assertThat(statusOf(scan(ean, location, "OUT"))).isEqualTo("OUT_OK");
        UUID empty = scan(ean, location, "OUT");
        assertThat(statusOf(empty)).isEqualTo("NOT_IN_STOCK");

        mvc.perform(get("/api/stock/movements").param("articleId", article.toString()).with(USER))
                .andExpect(jsonPath("$.items[0].source").value("MOBILE"));
        JsonNode event = kafka.await("stock_booking", in.toString(), 1).getFirst();
        assertThat(event.get("data").get("status").asString()).isEqualTo("IN_OK");
        mvc.perform(get("/api/stock/bookings/" + UUID.randomUUID()).with(USER)).andExpect(status().isNotFound());
    }

    @Test
    void unknownBarcodes() throws Exception {
        UUID location = location(unique("Keller"), null);
        String ean = ean();
        assertThat(statusOf(scan(ean, location, "OUT"))).isEqualTo("UNKNOWN_EAN");

        UUID pending = scan(ean, location, "IN");
        assertThat(statusOf(pending)).isEqualTo("PENDING_ARTICLE");
        UUID pending2 = scan(ean, location, "IN");

        // the catalog service creates an inbox placeholder -> pending bookings are completed
        article(UUID.randomUUID(), 0, false, "Unbekannt (" + ean + ")", "UNKNOWN", null, ean);
        mvc.perform(get("/api/stock/bookings/" + pending).with(USER))
                .andExpect(jsonPath("$.data.status").value("IN_OK_UNKNOWN_ARTICLE"))
                .andExpect(jsonPath("$.data.message").value(org.hamcrest.Matchers.containsString("Inbox")));
        assertThat(statusOf(pending2)).isEqualTo("IN_OK_UNKNOWN_ARTICLE");

        // the catalog found the product in a product database
        String ean2 = ean();
        UUID pending3 = scan(ean2, location, "IN");
        article(UUID.randomUUID(), 0, false, "Tee", "ACTIVE", null, ean2);
        assertThat(statusOf(pending3)).isEqualTo("IN_OK_NEW_ARTICLE");
    }

    @Test
    void deletedLocation() throws Exception {
        UUID location = location(unique("Weg"), null);
        String ean = ean();
        UUID pending = scan(ean, location, "IN");
        mvc.perform(delete("/api/stock/locations/" + location).param("version", "0").with(USER)).andExpect(status().isNoContent());
        assertThat(statusOf(scan(ean, location, "IN"))).isEqualTo("NO_LOCATION");
        article(UUID.randomUUID(), 0, false, "Brot", "ACTIVE", null, ean);
        assertThat(statusOf(pending)).isEqualTo("NO_LOCATION");
    }

    private static ScanEventDto scanEvent(UUID id, boolean deleted, String status, String ean, UUID location, String direction) {
        return new ScanEventDto(id, new AuditDto(Instant.now(), Instant.now(), 0L, deleted),
                new ScanDataDto(ean, direction, location, UUID.randomUUID(), status));
    }

    @Test
    void scansFromDevicesAreBookedOnce() throws Exception {
        UUID location = location(unique("Vorrat"), null);
        String ean = ean();
        article("Mehl", ean);

        // ignored
        UUID ignored = UUID.randomUUID();
        bookings.handleScan(scanEvent(ignored, true, "REQUESTED", ean, location, "IN"));
        bookings.handleScan(scanEvent(ignored, false, "PROCESSED", ean, location, "IN"));
        bookings.handleScan(scanEvent(ignored, false, "REQUESTED", null, location, "IN"));
        bookings.handleScan(scanEvent(ignored, false, "REQUESTED", ean, null, "IN"));
        bookings.handleScan(scanEvent(ignored, false, "REQUESTED", ean, location, null));
        bookings.handleScan(scanEvent(ignored, false, "REQUESTED", ean, location, "SIDEWAYS"));
        assertThat(repository.existsByScanId(ignored)).isFalse();

        // via Kafka, delivered twice
        UUID scan = UUID.randomUUID();
        String payload = """
                {"id":"%s","audit":{"createdDate":"2026-01-01T00:00:00Z","lastModifiedDate":"2026-01-01T00:00:00Z",
                 "version":0,"deleted":false},"data":{"ean":"%s","direction":"IN","locationId":"%s",
                 "scannerId":"%s","status":"REQUESTED","other":1}}""".formatted(scan, ean, location, UUID.randomUUID());
        kafka.send("device_scan", scan.toString(), payload);
        kafka.send("device_scan", scan.toString(), payload);
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> repository.existsByScanId(scan));
        JsonNode booking = kafka.await("stock_booking", repository.findAll().stream()
                .filter(b -> scan.equals(b.scanId())).findFirst().orElseThrow().id().toString(), 1).getFirst();
        assertThat(booking.get("data").get("source").asString()).isEqualTo("SCANNER");
        assertThat(booking.get("data").get("scanId").asString()).isEqualTo(scan.toString());
        Thread.sleep(1000);
        assertThat(repository.findAll().stream().filter(b -> scan.equals(b.scanId())).count()).isEqualTo(1);
    }
}
