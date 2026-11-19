package home.inventory.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import home.inventory.device.mqtt.MqttInboundService;
import home.inventory.device.scan.Scan;
import home.inventory.device.scan.ScanRepository;
import home.inventory.device.scan.ScanStatus;
import home.inventory.device.scanner.DeviceKind;
import home.inventory.device.scanner.Scanner;
import home.inventory.device.scanner.ScannerRepository;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

class DeviceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ScannerRepository scanners;

    @Autowired
    ScanRepository scans;

    @Autowired
    MqttInboundService inbound;

    private Scanner awaitScanner(String device) {
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> scanners.findByDeviceId(device).isPresent());
        return scanners.findByDeviceId(device).orElseThrow();
    }

    private Optional<Scan> scanOf(String device, long seq) {
        return scans.findAll().stream().filter(s -> s.deviceId().equals(device) && s.seq() == seq).findFirst();
    }

    private Scan awaitScan(String device, long seq) {
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> scanOf(device, seq).isPresent());
        return scanOf(device, seq).orElseThrow();
    }

    private void assign(Scanner scanner, UUID location) throws Exception {
        long version = scanners.findById(scanner.id()).orElseThrow().version();
        mvc.perform(json(put("/api/device/scanners/" + scanner.id()),
                        "{\"audit\":%s,\"data\":{\"name\":\" Vorrat \",\"locationId\":\"%s\",\"online\":false,\"buttons\":[]}}".formatted(audit(version), location)))
                .andExpect(status().isNoContent());
    }

    private void booking(UUID scanId, String status, String message) {
        UUID id = UUID.randomUUID();
        kafka.send("stock_booking", id.toString(), """
                {"id":"%s","audit":%s,"data":{"scanId":%s,"status":"%s","articleName":"Kaffee","quantityAtLocation":3,
                 "message":%s,"ean":"1"}}""".formatted(id, audit(0), scanId == null ? null : "\"" + scanId + "\"", status,
                message == null ? null : "\"" + message + "\""));
    }

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        mvc.perform(get("/api/device/scanners")).andExpect(status().isUnauthorized());
    }

    @Test
    void scannerRegistersAndIsManaged() throws Exception {
        String device = device();
        mqtt.publish("inventory/scanner/" + device + "/status", "{\"online\":true,\"fw\":\"0.2.0\",\"ip\":\"192.168.1.9\",\"rssi\":-50,\"x\":1}");
        Scanner scanner = awaitScanner(device);
        mvc.perform(get("/api/device/scanners").with(USER))
                .andExpect(jsonPath("$[?(@.deviceId=='" + device + "')].firmwareVersion").value(org.hamcrest.Matchers.contains("0.2.0")));

        // last will: offline keeps the last known firmware
        mqtt.publish("inventory/scanner/" + device + "/status", "{\"online\":false}");
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> !scanners.findByDeviceId(device).orElseThrow().online());
        mvc.perform(get("/api/device/scanners/" + scanner.id()).with(USER))
                .andExpect(jsonPath("$.data.firmwareVersion").value("0.2.0"))
                .andExpect(jsonPath("$.data.ipAddress").value("192.168.1.9"))
                .andExpect(jsonPath("$.audit.version").value(1));

        mvc.perform(json(put("/api/device/scanners/" + scanner.id()), "{\"audit\":%s,\"data\":{\"name\":\" \",\"online\":false,\"buttons\":[]}}".formatted(audit(0))))
                .andExpect(status().isConflict());
        mvc.perform(json(put("/api/device/scanners/" + scanner.id()), "{\"audit\":%s,\"data\":{\"name\":\" \",\"online\":false,\"buttons\":[]}}".formatted(audit(1))))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/device/scanners/" + scanner.id()).with(USER)).andExpect(jsonPath("$.data.name").doesNotExist());
        mvc.perform(json(put("/api/device/scanners/" + scanner.id()), "{\"audit\":%s,\"data\":{\"online\":false,\"buttons\":[]}}".formatted(audit(2))))
                .andExpect(status().isNoContent());

        assign(scanner, UUID.randomUUID());
        mvc.perform(get("/api/device/scanners/" + scanner.id()).with(USER)).andExpect(jsonPath("$.data.name").value("Vorrat"));
        long version = scanners.findById(scanner.id()).orElseThrow().version();
        mvc.perform(delete("/api/device/scanners/" + scanner.id()).param("version", String.valueOf(version)).with(USER))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/device/scanners").with(USER))
                .andExpect(jsonPath("$[?(@.deviceId=='" + device + "')]").isEmpty());

        // reconnect after removal: registered again, without location
        mqtt.publish("inventory/scanner/" + device + "/status", "{\"online\":true}");
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> !scanners.findByDeviceId(device).orElseThrow().deleted());
        assertThat(scanners.findByDeviceId(device).orElseThrow().locationId()).isNull();
        mvc.perform(get("/api/device/scanners/" + UUID.randomUUID()).with(USER)).andExpect(status().isNotFound());

        JsonNode event = kafka.await("device_scanner", scanner.id().toString(), 6).getLast();
        assertThat(event.get("data").get("deviceId").asString()).isEqualTo(device);
    }

    @Test
    void scansAreForwardedAndAnswered() throws Exception {
        String device = device();
        String topic = "inventory/scanner/" + device + "/scan";

        // unknown scanner without location: registered, rejected
        mqtt.publish(topic, "{\"ean\":\"4006381333931\",\"direction\":\"in\",\"seq\":1,\"bootId\":\"b1\"}");
        JsonNode noLocation = mqtt.feedback(device, 1).getFirst();
        assertThat(noLocation.get("result").asString()).isEqualTo("NO_LOCATION");
        assertThat(noLocation.get("success").asBoolean()).isFalse();
        assertThat(awaitScan(device, 1).status()).isEqualTo(ScanStatus.REJECTED);

        UUID location = UUID.randomUUID();
        assign(awaitScanner(device), location);

        mqtt.publish(topic, "{\"ean\":\" 4006381333931 \",\"direction\":\"IN\",\"seq\":2,\"bootId\":\"b1\"}");
        Scan scan = awaitScan(device, 2);
        assertThat(scan.status()).isEqualTo(ScanStatus.REQUESTED);
        assertThat(scan.locationId()).isEqualTo(location);
        JsonNode event = kafka.await("device_scan", scan.id().toString(), 1).getFirst();
        assertThat(event.get("data").get("status").asString()).isEqualTo("REQUESTED");
        assertThat(event.get("data").get("ean").asString()).isEqualTo("4006381333931");

        // redelivery is ignored
        mqtt.publish(topic, "{\"ean\":\"4006381333931\",\"direction\":\"IN\",\"seq\":2,\"bootId\":\"b1\"}");
        // invalid messages
        mqtt.publish(topic, "{\"ean\":\" \",\"direction\":\"IN\",\"seq\":3,\"bootId\":\"b1\"}");
        mqtt.publish(topic, "{\"ean\":\"1\",\"direction\":\"UP\",\"seq\":4,\"bootId\":\"b1\"}");
        mqtt.publish(topic, "{\"ean\":\"1\",\"seq\":5,\"bootId\":\"b1\"}");
        mqtt.publish(topic, "{\"direction\":\"IN\",\"seq\":6,\"bootId\":\"b1\"}");
        mqtt.publish(topic, "{\"ean\":\"1\",\"direction\":\"IN\",\"bootId\":\"b1\"}");
        mqtt.publish(topic, "{\"ean\":\"1\",\"direction\":\"IN\",\"seq\":7}");
        mqtt.publish(topic, "not json");
        List<JsonNode> errors = mqtt.feedback(device, 8);
        assertThat(errors.subList(1, 8)).allSatisfy(f -> assertThat(f.get("result").asString()).isEqualTo("ERROR"));
        assertThat(scans.findAll().stream().filter(s -> s.deviceId().equals(device)).count()).isEqualTo(2);

        // stock: still waiting for the article -> scanner keeps waiting
        booking(scan.id(), "REQUESTED", null);
        booking(scan.id(), "PENDING_ARTICLE", null);
        JsonNode pending = mqtt.feedback(device, 9).get(8);
        assertThat(pending.get("result").asString()).isEqualTo("PENDING_ARTICLE");
        assertThat(pending.get("message").asString()).isEmpty();
        assertThat(scans.findById(scan.id()).orElseThrow().status()).isEqualTo(ScanStatus.REQUESTED);

        booking(scan.id(), "IN_OK_UNKNOWN_ARTICLE", "eingelagert");
        JsonNode done = mqtt.feedback(device, 10).get(9);
        assertThat(done.get("success").asBoolean()).isTrue();
        assertThat(done.get("seq").asLong()).isEqualTo(2);
        assertThat(done.get("quantity").asLong()).isEqualTo(3);
        Awaitility.await().atMost(Duration.ofSeconds(10))
                .until(() -> scans.findById(scan.id()).orElseThrow().status() == ScanStatus.PROCESSED);

        // ignored: already processed, foreign bookings, unknown scans
        booking(scan.id(), "OUT_OK", "x");
        booking(null, "IN_OK", "x");
        booking(UUID.randomUUID(), "IN_OK", "x");
        Thread.sleep(1500);
        assertThat(mqtt.feedbackCount(device)).isEqualTo(10);
        assertThat(scans.findById(scan.id()).orElseThrow().result()).isEqualTo("IN_OK_UNKNOWN_ARTICLE");
    }

    @Test
    void unusableTopicsAreIgnored() throws Exception {
        inbound.handle("x", "{}");
        String device = device();
        inbound.handle("inventory/scanner/" + device + "/other", "{}");
        inbound.handle("inventory/scanner/" + device + "/status", "no json");
        assertThat(scanners.findByDeviceId(device)).isEmpty();
        assertThat(mqtt.feedbackCount(device)).isZero();
    }

    @Test
    void barcodesAreCheckedAndNormalized() throws Exception {
        String device = device();
        String topic = "inventory/scanner/" + device + "/scan";
        mqtt.publish(topic, "{\"ean\":\"036000291452\",\"direction\":\"IN\",\"seq\":1,\"bootId\":\"b1\"}");
        mqtt.feedback(device, 1); // no location yet
        assign(awaitScanner(device), UUID.randomUUID());

        // invalid check digit: rejected, scanner gets INVALID_EAN
        mqtt.publish(topic, "{\"ean\":\"4006381333932\",\"direction\":\"IN\",\"seq\":2,\"bootId\":\"b1\"}");
        JsonNode invalid = mqtt.feedback(device, 2).get(1);
        assertThat(invalid.get("result").asString()).isEqualTo("INVALID_EAN");
        assertThat(invalid.get("success").asBoolean()).isFalse();
        assertThat(invalid.get("message").asString()).startsWith("Ungültige EAN");
        Scan rejected = awaitScan(device, 2);
        assertThat(rejected.status()).isEqualTo(ScanStatus.REJECTED);
        assertThat(rejected.ean()).isEqualTo("4006381333932");

        // UPC-A is forwarded as EAN-13
        mqtt.publish(topic, "{\"ean\":\"036000291452\",\"direction\":\"IN\",\"seq\":3,\"bootId\":\"b1\"}");
        Scan scan = awaitScan(device, 3);
        assertThat(scan.ean()).isEqualTo("0036000291452");
        JsonNode event = kafka.await("device_scan", scan.id().toString(), 1).getFirst();
        assertThat(event.get("data").get("ean").asString()).isEqualTo("0036000291452");
    }

    private void configureButtons(Scanner scanner, UUID location, String buttons) throws Exception {
        long version = scanners.findById(scanner.id()).orElseThrow().version();
        mvc.perform(json(put("/api/device/scanners/" + scanner.id()),
                        "{\"audit\":%s,\"data\":{\"name\":\"Gemüse\",\"locationId\":%s,\"online\":false,\"buttons\":%s}}"
                                .formatted(audit(version), location == null ? null : "\"" + location + "\"", buttons)))
                .andExpect(status().isNoContent());
    }

    @Test
    void buttonBoxesBookTheAssignedArticle() throws Exception {
        String device = device();
        String prefix = "inventory/scanner/" + device + "/";
        mqtt.publish(prefix + "status", "{\"online\":true,\"fw\":\"1.0\",\"kind\":\"buttons\",\"bootId\":\"b1\"}");
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> scanners.findByDeviceId(device)
                .map(s -> s.kind() == DeviceKind.BUTTONS).orElse(false));
        Scanner box = awaitScanner(device);

        // no location yet
        mqtt.publish(prefix + "button", "{\"button\":1,\"direction\":\"IN\",\"seq\":1,\"bootId\":\"b1\"}");
        assertThat(mqtt.feedback(device, 1).getFirst().get("result").asString()).isEqualTo("NO_LOCATION");

        UUID location = UUID.randomUUID();
        UUID tomatoes = UUID.randomUUID();
        UUID cucumbers = UUID.randomUUID();
        configureButtons(box, location, "[{\"button\":1,\"articleId\":\"%s\"},{\"button\":3,\"articleId\":\"%s\"}]"
                .formatted(tomatoes, cucumbers));
        mvc.perform(get("/api/device/scanners/" + box.id()).with(USER))
                .andExpect(jsonPath("$.data.kind").value("BUTTONS"))
                .andExpect(jsonPath("$.data.buttons[0].button").value(1))
                .andExpect(jsonPath("$.data.buttons[1].articleId").value(cucumbers.toString()));
        mvc.perform(get("/api/device/scanners").with(USER))
                .andExpect(jsonPath("$[?(@.deviceId=='" + device + "')].buttons.length()").value(2));

        // short press on button 3 -> IN for the cucumbers, forwarded to the stock service
        mqtt.publish(prefix + "button", "{\"button\":3,\"direction\":\"in\",\"seq\":2,\"bootId\":\"b1\"}");
        Scan press = awaitScan(device, 2);
        assertThat(press.ean()).isNull();
        assertThat(press.button()).isEqualTo(3);
        assertThat(press.articleId()).isEqualTo(cucumbers);
        JsonNode event = kafka.await("device_scan", press.id().toString(), 1).getFirst();
        assertThat(event.get("data").get("articleId").asString()).isEqualTo(cucumbers.toString());
        assertThat(event.get("data").get("direction").asString()).isEqualTo("IN");
        booking(press.id(), "IN_OK", "Gurken eingelagert");
        assertThat(mqtt.feedback(device, 2).get(1).get("success").asBoolean()).isTrue();

        // button without article
        mqtt.publish(prefix + "button", "{\"button\":2,\"direction\":\"OUT\",\"seq\":3,\"bootId\":\"b1\"}");
        JsonNode noArticle = mqtt.feedback(device, 3).get(2);
        assertThat(noArticle.get("result").asString()).isEqualTo("NO_ARTICLE");
        assertThat(noArticle.get("message").asString()).isEqualTo("Taster 2 ist keinem Artikel zugeordnet");
        assertThat(awaitScan(device, 3).status()).isEqualTo(ScanStatus.REJECTED);

        // redelivery ignored; invalid messages
        mqtt.publish(prefix + "button", "{\"button\":3,\"direction\":\"IN\",\"seq\":2,\"bootId\":\"b1\"}");
        mqtt.publish(prefix + "button", "{\"direction\":\"IN\",\"seq\":4,\"bootId\":\"b1\"}");
        mqtt.publish(prefix + "button", "{\"button\":0,\"direction\":\"IN\",\"seq\":5,\"bootId\":\"b1\"}");
        mqtt.publish(prefix + "button", "{\"button\":9,\"direction\":\"IN\",\"seq\":6,\"bootId\":\"b1\"}");
        mqtt.publish(prefix + "button", "{\"button\":1,\"seq\":7,\"bootId\":\"b1\"}");
        mqtt.publish(prefix + "button", "{\"button\":1,\"direction\":\"IN\",\"bootId\":\"b1\"}");
        mqtt.publish(prefix + "button", "{\"button\":1,\"direction\":\"IN\",\"seq\":8}");
        mqtt.publish(prefix + "button", "kaputt");
        List<JsonNode> errors = mqtt.feedback(device, 10);
        assertThat(errors.subList(3, 10)).allSatisfy(f -> assertThat(f.get("result").asString()).isEqualTo("ERROR"));

        // a button can only be assigned once; offline status keeps the kind
        long version = scanners.findById(box.id()).orElseThrow().version();
        mvc.perform(json(put("/api/device/scanners/" + box.id()),
                        "{\"audit\":%s,\"data\":{\"online\":false,\"buttons\":[{\"button\":1,\"articleId\":\"%s\"},{\"button\":1,\"articleId\":\"%s\"}]}}"
                                .formatted(audit(version), tomatoes, cucumbers)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Taster 1 ist doppelt belegt"));
        mqtt.publish(prefix + "status", "{\"online\":false}");
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> !scanners.findByDeviceId(device).orElseThrow().online());
        assertThat(scanners.findByDeviceId(device).orElseThrow().kind()).isEqualTo(DeviceKind.BUTTONS);
        // a status with another kind name counts as scanner
        assertThat(DeviceKind.fromStatus("scanner")).isEqualTo(DeviceKind.SCANNER);
    }
}
