package home.inventory.device.ota;

import home.inventory.device.common.AuditMapper;
import home.inventory.device.common.ConflictException;
import home.inventory.device.common.EventPublisher;
import home.inventory.device.common.NotFoundException;
import home.inventory.device.common.OutgoingEvent;
import home.inventory.device.config.MqttProperties;
import home.inventory.device.config.Topics;
import home.inventory.device.firmware.Firmware;
import home.inventory.device.firmware.FirmwareService;
import home.inventory.device.mqtt.MqttPublisher;
import home.inventory.device.mqtt.dto.OtaCommandDto;
import home.inventory.device.mqtt.dto.OtaStateMessageDto;
import home.inventory.device.ota.dto.OtaJobDataDto;
import home.inventory.device.ota.dto.OtaJobDto;
import home.inventory.device.ota.dto.OtaJobEventDto;
import home.inventory.device.ota.dto.OtaRolloutDto;
import home.inventory.device.scanner.DeviceKind;
import home.inventory.device.scanner.Scanner;
import home.inventory.device.scanner.ScannerRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Firmware updates over the air: the device gets a command via MQTT, downloads the image from the download
 * server with a one-time token and installs it only if the SHA-256 matches. After the restart its status
 * message reports the new version.
 */
@Service
@Transactional
public class OtaService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtaJobRepository repository;
    private final ScannerRepository scanners;
    private final FirmwareService firmware;
    private final MqttPublisher mqtt;
    private final MqttProperties mqttProperties;
    private final OtaDownloadPort downloadPort;
    private final EventPublisher events;
    private final ObjectMapper json;
    private final Clock clock;

    public OtaService(OtaJobRepository repository, ScannerRepository scanners, FirmwareService firmware,
                      MqttPublisher mqtt, MqttProperties mqttProperties, OtaDownloadPort downloadPort,
                      EventPublisher events, ObjectMapper json, Clock clock) {
        this.repository = repository;
        this.scanners = scanners;
        this.firmware = firmware;
        this.mqtt = mqtt;
        this.mqttProperties = mqttProperties;
        this.downloadPort = downloadPort;
        this.events = events;
        this.json = json;
        this.clock = clock;
    }

    /** Starts the update of one device to the given firmware (default: newest of its kind). */
    public UUID start(UUID scannerId, @Nullable UUID firmwareId) {
        Scanner scanner = scanners.findById(scannerId).filter(s -> !s.deleted())
                .orElseThrow(() -> new NotFoundException("Gerät", scannerId));
        Firmware image = firmwareId != null ? firmware.load(firmwareId) : firmware.latest(scanner.kind())
                .orElseThrow(() -> new ConflictException("Keine Firmware für diese Geräteart hochgeladen"));
        if (image.kind() != scanner.kind()) {
            throw new ConflictException("Firmware passt nicht zur Geräteart");
        }
        if (!scanner.online()) {
            throw new ConflictException("Gerät " + scanner.displayName() + " ist offline");
        }
        byte[] token = new byte[24];
        RANDOM.nextBytes(token);
        OtaJob job = save(OtaJob.create(scanner.id(), scanner.deviceId(), image.id(), image.firmwareVersion(),
                Base64.getUrlEncoder().withoutPadding().encodeToString(token),
                Instant.now(clock).plus(downloadPort.tokenValidity())));
        mqtt.publish(mqttProperties.topicPrefix() + "/" + scanner.deviceId() + "/ota",
                json.writeValueAsString(new OtaCommandDto(job.id(), image.firmwareVersion(), image.size(),
                        image.sha256(), downloadPort.port(), "/ota/" + job.token())));
        return job.id();
    }

    /** Updates every online device of a kind that does not run the newest firmware yet. */
    public OtaRolloutDto startAll(DeviceKind kind) {
        Firmware image = firmware.latest(kind)
                .orElseThrow(() -> new ConflictException("Keine Firmware für diese Geräteart hochgeladen"));
        int started = 0;
        for (Scanner s : scanners.findAllByDeletedFalseOrderByDeviceIdAsc()) {
            if (s.kind() == kind && s.online() && !image.firmwareVersion().equals(s.firmwareVersion())) {
                start(s.id(), image.id());
                started++;
            }
        }
        return new OtaRolloutDto(started);
    }

    /** Progress reported by the device ({@code …/otastate}). */
    public void handleState(String deviceId, OtaStateMessageDto message) {
        if (message.job() == null || message.state() == null) {
            return;
        }
        repository.findById(message.job())
                .filter(job -> job.deviceId().equals(deviceId) && !job.state().finished())
                .ifPresent(job -> {
                    switch (message.state().toLowerCase(Locale.ROOT)) {
                        case "downloading" -> save(job.withState(OtaState.DOWNLOADING, null));
                        case "installed" -> save(job.withState(OtaState.INSTALLED, null));
                        case "failed" -> save(job.withState(OtaState.FAILED,
                                message.error() == null ? "Fehler auf dem Gerät" : message.error()));
                        default -> { }
                    }
                });
    }

    /** Status message of a device after (re)connecting: confirms a running update. */
    public void statusReported(Scanner scanner, @Nullable String firmwareVersion) {
        repository.findFirstByScannerIdOrderByCreatedDateDesc(scanner.id())
                .filter(job -> !job.state().finished() && firmwareVersion != null)
                .ifPresent(job -> {
                    if (job.targetVersion().equals(firmwareVersion)) {
                        save(job.withState(OtaState.SUCCESS, null));
                    } else if (job.state() == OtaState.INSTALLED) {
                        save(job.withState(OtaState.FAILED, "Gerät meldet nach dem Neustart Version " + firmwareVersion));
                    }
                });
    }

    /** Image for a download token; empty if the token is unknown, used up or expired. */
    @Transactional(readOnly = true)
    public Optional<byte[]> download(String token) {
        return repository.findByToken(token)
                .filter(job -> !job.state().finished() && !Instant.now(clock).isAfter(job.expiresAt()))
                .map(job -> firmware.content(job.firmwareId()));
    }

    /** Last update of a device; unfinished updates past their deadline count as failed. */
    @Transactional(readOnly = true)
    public Optional<OtaJobDto> lastJob(UUID scannerId) {
        return repository.findFirstByScannerIdOrderByCreatedDateDesc(scannerId).map(job -> {
            boolean timedOut = !job.state().finished() && Instant.now(clock).isAfter(job.expiresAt());
            return new OtaJobDto(job.id(), job.targetVersion(), timedOut ? OtaState.FAILED : job.state(),
                    timedOut ? "Zeitüberschreitung – keine Rückmeldung vom Gerät" : job.error(), job.lastModifiedDate());
        });
    }

    private OtaJob save(OtaJob job) {
        OtaJob saved = repository.save(job);
        events.publish(new OutgoingEvent(Topics.OTA_JOB, saved.id(), new OtaJobEventDto(saved.id(),
                AuditMapper.toDto(saved), new OtaJobDataDto(saved.scannerId(), saved.deviceId(), saved.firmwareId(),
                saved.targetVersion(), saved.expiresAt(), saved.state(), saved.error()))));
        return saved;
    }
}
