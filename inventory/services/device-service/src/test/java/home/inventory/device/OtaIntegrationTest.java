package home.inventory.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import home.inventory.device.firmware.FirmwareRepository;
import home.inventory.device.ota.OtaDownloadPort;
import home.inventory.device.ota.OtaJobRepository;
import home.inventory.device.ota.OtaState;
import home.inventory.device.scanner.Scanner;
import home.inventory.device.scanner.ScannerRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.JsonNode;

class OtaIntegrationTest extends AbstractIntegrationTest {

    private static final HttpClient HTTP = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build();

    @Autowired
    ScannerRepository scanners;

    @Autowired
    OtaJobRepository jobs;

    @Autowired
    FirmwareRepository firmware;

    @Autowired
    OtaDownloadPort downloadPort;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    home.inventory.device.firmware.FirmwareService firmwareService;

    private String upload(String marker) throws Exception {
        String location = mvc.perform(multipart("/api/device/firmware")
                        .file(new MockMultipartFile("file", "firmware.bin", "application/octet-stream",
                                FirmwareImagesTest.image(marker)))
                        .with(USER))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private Scanner online(String device, String kind, String fw) throws Exception {
        mqtt.publish("inventory/scanner/" + device + "/status",
                "{\"online\":true,\"fw\":\"%s\",\"kind\":\"%s\"}".formatted(fw, kind));
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> scanners.findByDeviceId(device)
                .map(s -> fw.equals(s.firmwareVersion()) && s.online()).orElse(false));
        return scanners.findByDeviceId(device).orElseThrow();
    }

    private void state(String device, String payload) throws Exception {
        mqtt.publish("inventory/scanner/" + device + "/otastate", payload);
    }

    private OtaState awaitState(UUID job, OtaState expected) {
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> jobs.findById(job).orElseThrow().state() == expected);
        return expected;
    }

    private HttpResponse<byte[]> download(String path, String method) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:" + downloadPort.port() + path))
                .method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private UUID start(Scanner scanner) throws Exception {
        String location = mvc.perform(post("/api/device/scanners/" + scanner.id() + "/firmware-update").with(USER))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getHeader("Location");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

    @Test
    void uploadInstallAndConfirm() throws Exception {
        String version = "9." + System.nanoTime();
        String id = upload("INVFW|scanner|" + version + "|");
        mvc.perform(get("/api/device/firmware").with(USER))
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].firmwareVersion").value(version))
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].kind").value("SCANNER"));
        // same version again, not a firmware
        mvc.perform(multipart("/api/device/firmware").file(new MockMultipartFile("file", "x.bin", null,
                        FirmwareImagesTest.image("INVFW|scanner|" + version + "|"))).with(USER))
                .andExpect(status().isConflict());
        mvc.perform(multipart("/api/device/firmware").file(new MockMultipartFile("file", "x.txt", null,
                        "hello".getBytes())).with(USER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("Keine ESP32-Firmware")));
        assertThat(kafka.await("device_firmware", id, 1)).isNotEmpty();

        String device = device();
        Scanner scanner = online(device, "scanner", "0.1.0");
        UUID job = start(scanner);
        JsonNode command = mqtt.otaCommands(device, 1).getFirst();
        assertThat(command.get("job").asString()).isEqualTo(job.toString());
        assertThat(command.get("version").asString()).isEqualTo(version);
        assertThat(command.get("port").asInt()).isEqualTo(downloadPort.port());
        String path = command.get("path").asString();

        // download server: right token, wrong token, wrong method/path
        HttpResponse<byte[]> image = download(path, "GET");
        assertThat(image.statusCode()).isEqualTo(200);
        assertThat(image.body()).isEqualTo(FirmwareImagesTest.image("INVFW|scanner|" + version + "|"));
        assertThat(download("/ota/unknown", "GET").statusCode()).isEqualTo(404);
        assertThat(download(path, "POST").statusCode()).isEqualTo(404);
        assertThat(download("/other", "GET").statusCode()).isEqualTo(404);

        // progress reported by the device, then the restart with the new version
        state(device, "{\"job\":\"%s\",\"state\":\"downloading\"}".formatted(job));
        awaitState(job, OtaState.DOWNLOADING);
        state(device, "{\"job\":\"%s\",\"state\":\"installed\"}".formatted(job));
        awaitState(job, OtaState.INSTALLED);
        online(device, "scanner", version);
        awaitState(job, OtaState.SUCCESS);
        mvc.perform(get("/api/device/scanners").with(USER))
                .andExpect(jsonPath("$[?(@.deviceId=='" + device + "')].ota.state").value("SUCCESS"))
                .andExpect(jsonPath("$[?(@.deviceId=='" + device + "')].ota.targetVersion").value(version));
        assertThat(download(path, "GET").statusCode()).isEqualTo(404); // finished: token no longer valid
        assertThat(kafka.await("device_ota_job", job.toString(), 4)).hasSizeGreaterThanOrEqualTo(4);

        // delete the firmware
        long fwVersion = firmware.findById(UUID.fromString(id)).orElseThrow().version();
        mvc.perform(delete("/api/device/firmware/" + id).param("version", String.valueOf(fwVersion)).with(USER))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/device/scanners/" + scanner.id() + "/firmware-update")
                        .param("firmwareId", id).with(USER))
                .andExpect(status().isNotFound());
    }

    @Test
    void failuresAndTimeouts() throws Exception {
        String version = "8." + System.nanoTime();
        String buttonsFirmware = upload("INVFW|buttons|" + version + "|");
        String device = device();
        Scanner box = online(device, "buttons", "0.1.0");

        // explicit firmware of the wrong kind, unknown device, offline device
        String scannerFw = upload("INVFW|scanner|" + version + "|");
        mvc.perform(post("/api/device/scanners/" + box.id() + "/firmware-update").param("firmwareId", scannerFw).with(USER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Firmware passt nicht zur Geräteart"));
        mvc.perform(post("/api/device/scanners/" + UUID.randomUUID() + "/firmware-update").with(USER))
                .andExpect(status().isNotFound());

        // device reports a failure (with and without text)
        UUID job = UUID.fromString(mvc.perform(post("/api/device/scanners/" + box.id() + "/firmware-update")
                        .param("firmwareId", buttonsFirmware).with(USER))
                .andReturn().getResponse().getHeader("Location").replaceAll(".*/", ""));
        state(device, "{\"state\":\"failed\"}");                                        // no job: ignored
        state("other-device", "{\"job\":\"%s\",\"state\":\"failed\"}".formatted(job)); // other device: ignored
        state(device, "{\"job\":\"%s\",\"state\":\"rebooting\"}".formatted(job));     // unknown state: ignored
        state(device, "{\"job\":\"%s\"}".formatted(job));                               // no state: ignored
        online(device, "buttons", "0.1.1");                                                // still requested, other version
        state(device, "{\"job\":\"%s\",\"state\":\"failed\",\"error\":\"Prüfsumme falsch\"}".formatted(job));
        awaitState(job, OtaState.FAILED);
        assertThat(jobs.findById(job).orElseThrow().error()).isEqualTo("Prüfsumme falsch");
        state(device, "{\"job\":\"%s\",\"state\":\"downloading\"}".formatted(job)); // finished: ignored

        online(device, "buttons", "0.1.5");                                                // last job finished: nothing
        assertThat(jobs.findById(job).orElseThrow().state()).isEqualTo(OtaState.FAILED);

        UUID second = start(box);
        state(device, "{\"job\":\"%s\",\"state\":\"failed\"}".formatted(second));
        awaitState(second, OtaState.FAILED);
        assertThat(jobs.findById(second).orElseThrow().error()).isEqualTo("Fehler auf dem Gerät");

        // installed, but the device comes back with the old version
        UUID third = start(box);
        state(device, "{\"job\":\"%s\",\"state\":\"installed\"}".formatted(third));
        awaitState(third, OtaState.INSTALLED);
        mqtt.publish("inventory/scanner/" + device + "/status", "{\"online\":true}"); // no version: nothing
        online(device, "buttons", "0.1.2");
        awaitState(third, OtaState.FAILED);
        assertThat(jobs.findById(third).orElseThrow().error()).contains("0.1.2");

        // no answer in time
        UUID fourth = start(box);
        String token = jobs.findById(fourth).orElseThrow().token();
        jdbc.update("update ota_job set expires_at = ? where id = ?", Timestamp.from(Instant.now().minusSeconds(60)), fourth);
        assertThat(download("/ota/" + token, "GET").statusCode()).isEqualTo(404);
        mvc.perform(get("/api/device/scanners").with(USER))
                .andExpect(jsonPath("$[?(@.deviceId=='" + device + "')].ota.state").value("FAILED"))
                .andExpect(jsonPath("$[?(@.deviceId=='" + device + "')].ota.error")
                        .value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.startsWith("Zeitüberschreitung"))));

        // image missing (should not happen) and removed device
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> firmwareService.content(UUID.randomUUID()))
                .isInstanceOf(home.inventory.device.common.NotFoundException.class);
        long boxVersion = scanners.findById(box.id()).orElseThrow().version();
        mvc.perform(delete("/api/device/scanners/" + box.id()).param("version", String.valueOf(boxVersion)).with(USER))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/device/scanners/" + box.id() + "/firmware-update").with(USER))
                .andExpect(status().isNotFound());
        online(device, "buttons", "0.1.6"); // registers again

        // offline
        mqtt.publish("inventory/scanner/" + device + "/status", "{\"online\":false}");
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> !scanners.findByDeviceId(device).orElseThrow().online());
        mvc.perform(post("/api/device/scanners/" + box.id() + "/firmware-update").with(USER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.endsWith("ist offline")));
    }

    @Test
    void rolloutUpdatesOnlineDevicesThatNeedIt() throws Exception {
        jdbc.update("update firmware set deleted = true");
        mvc.perform(post("/api/device/firmware-updates").param("kind", "SCANNER").with(USER))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/device/scanners/" + online(device(), "scanner", "0.0.1").id() + "/firmware-update").with(USER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Keine Firmware für diese Geräteart hochgeladen"));

        String version = "7." + System.nanoTime();
        upload("INVFW|scanner|" + version + "|");
        String old = device();
        String current = device();
        String offline = device();
        String box = device();
        online(old, "scanner", "0.0.1");
        online(current, "scanner", version);
        online(offline, "scanner", "0.0.1");
        online(box, "buttons", "0.0.1");
        mqtt.publish("inventory/scanner/" + offline + "/status", "{\"online\":false}");
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> !scanners.findByDeviceId(offline).orElseThrow().online());
        jdbc.update("update scanner set online = false where device_id not in (?, ?, ?)", old, current, box);

        mvc.perform(post("/api/device/firmware-updates").param("kind", "SCANNER").with(USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.started").value(1));
        assertThat(mqtt.otaCommands(old, 1)).hasSize(1);
    }
}
