package home.inventory.device.firmware;

import home.inventory.device.common.AuditMapper;
import home.inventory.device.common.ConflictException;
import home.inventory.device.common.EventPublisher;
import home.inventory.device.common.NotFoundException;
import home.inventory.device.common.OutgoingEvent;
import home.inventory.device.common.VersionCheck;
import home.inventory.device.config.Topics;
import home.inventory.device.firmware.dto.FirmwareDataDto;
import home.inventory.device.firmware.dto.FirmwareEventDto;
import home.inventory.device.firmware.dto.FirmwareListDto;
import home.inventory.device.scanner.DeviceKind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Firmware images uploaded in the web app, installed on the devices via OTA. */
@Service
@Transactional
public class FirmwareService {

    private final FirmwareRepository repository;
    private final FirmwareContentRepository contents;
    private final EventPublisher events;

    public FirmwareService(FirmwareRepository repository, FirmwareContentRepository contents, EventPublisher events) {
        this.repository = repository;
        this.contents = contents;
        this.events = events;
    }

    public UUID upload(byte[] image, String fileName) {
        FirmwareImages.Info info = FirmwareImages.inspect(image);
        if (repository.existsByKindAndFirmwareVersionAndDeletedFalse(info.kind(), info.version())) {
            throw new ConflictException("Version " + info.version() + " ist bereits hochgeladen");
        }
        Firmware saved = save(Firmware.create(info.kind(), info.version(), image.length, info.sha256(), fileName));
        contents.save(FirmwareContent.create(saved.id(), image));
        return saved.id();
    }

    @Transactional(readOnly = true)
    public List<FirmwareListDto> list() {
        return repository.findAllByDeletedFalseOrderByCreatedDateDesc().stream()
                .map(f -> new FirmwareListDto(f.id(), f.kind(), f.firmwareVersion(), f.size(), f.sha256(),
                        f.fileName(), f.createdDate(), f.version()))
                .toList();
    }

    public void delete(UUID id, Long version) {
        save(VersionCheck.require(load(id), version).withDeleted(true));
    }

    /** Newest firmware of a device kind. */
    @Transactional(readOnly = true)
    public Optional<Firmware> latest(DeviceKind kind) {
        return repository.findFirstByKindAndDeletedFalseOrderByCreatedDateDesc(kind);
    }

    @Transactional(readOnly = true)
    public Firmware load(UUID id) {
        return repository.findById(id).filter(f -> !f.deleted())
                .orElseThrow(() -> new NotFoundException("Firmware", id));
    }

    @Transactional(readOnly = true)
    public byte[] content(UUID firmwareId) {
        return contents.findByFirmwareId(firmwareId).orElseThrow(() -> new NotFoundException("Firmware", firmwareId))
                .content();
    }

    private Firmware save(Firmware firmware) {
        Firmware saved = repository.save(firmware);
        events.publish(new OutgoingEvent(Topics.FIRMWARE, saved.id(), new FirmwareEventDto(saved.id(),
                AuditMapper.toDto(saved), new FirmwareDataDto(saved.kind(), saved.firmwareVersion(), saved.size(),
                saved.sha256(), saved.fileName()))));
        return saved;
    }
}
