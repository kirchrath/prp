package home.inventory.device.scanner;

import home.inventory.device.common.AuditMapper;
import home.inventory.device.common.ConflictException;
import home.inventory.device.common.EventPublisher;
import home.inventory.device.common.NotFoundException;
import home.inventory.device.common.OutgoingEvent;
import home.inventory.device.common.VersionCheck;
import home.inventory.device.config.Topics;
import home.inventory.device.mqtt.dto.StatusMessageDto;
import home.inventory.device.scanner.dto.ScannerDataDto;
import home.inventory.device.scanner.dto.ScannerDto;
import home.inventory.device.scanner.dto.ScannerEventDto;
import home.inventory.device.scanner.dto.ScannerListDto;
import home.inventory.device.scanner.dto.ScannerSaveDto;
import home.inventory.device.scanner.dto.ButtonDto;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ScannerService {

    private final ScannerRepository repository;
    private final EventPublisher events;

    public ScannerService(ScannerRepository repository, EventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<ScannerListDto> list() {
        return repository.findAllByDeletedFalseOrderByDeviceIdAsc().stream()
                .map(s -> new ScannerListDto(s.id(), s.deviceId(), s.name(), s.locationId(), s.online(),
                        s.firmwareVersion(), s.ipAddress(), s.lastSeen(), s.kind(), buttons(s)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ScannerDto get(UUID id) {
        Scanner s = load(id);
        return new ScannerDto(AuditMapper.toDto(s), data(s));
    }

    /** Only name, location and the articles of the buttons can be changed. */
    public void update(UUID id, ScannerSaveDto request) {
        Scanner current = VersionCheck.require(load(id), request.audit().version());
        String name = request.data().name();
        Set<ButtonAssignment> buttons = new HashSet<>();
        Set<Integer> numbers = new HashSet<>();
        for (ButtonDto b : request.data().buttons()) {
            if (!numbers.add(b.button())) {
                throw new ConflictException("Taster " + b.button() + " ist doppelt belegt");
            }
            buttons.add(new ButtonAssignment(b.button(), b.articleId()));
        }
        save(current.withAssignment(name == null || name.isBlank() ? null : name.trim(), request.data().locationId(),
                buttons));
    }

    /** Removes the scanner from the list; it registers again on its next connect. */
    public void delete(UUID id, Long version) {
        save(VersionCheck.require(load(id), version).withDeleted(true));
    }

    public void handleStatus(String deviceId, StatusMessageDto status) {
        save(find(deviceId).seen(status.online(), status.fw(), status.ip(), DeviceKind.fromStatus(status.kind()),
                Instant.now()));
    }

    /** The scanner with this device id; unknown devices are registered. */
    public Scanner register(String deviceId) {
        Scanner scanner = find(deviceId);
        return scanner.id() != null ? scanner : save(scanner.seen(true, null, null, null, Instant.now()));
    }

    public Scanner save(Scanner scanner) {
        Scanner saved = repository.save(scanner);
        events.publish(new OutgoingEvent(Topics.SCANNER, saved.id(),
                new ScannerEventDto(saved.id(), AuditMapper.toDto(saved), data(saved))));
        return saved;
    }

    private Scanner find(String deviceId) {
        return repository.findByDeviceId(deviceId).orElseGet(() -> Scanner.create(deviceId));
    }

    private Scanner load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Scanner", id));
    }

    private static ScannerDataDto data(Scanner s) {
        return new ScannerDataDto(s.deviceId(), s.name(), s.locationId(), s.online(), s.firmwareVersion(),
                s.ipAddress(), s.lastSeen(), s.kind(), buttons(s));
    }

    private static List<ButtonDto> buttons(Scanner s) {
        return s.buttons().stream()
                .sorted(Comparator.comparingInt(ButtonAssignment::button))
                .map(b -> new ButtonDto(b.button(), b.articleId()))
                .toList();
    }
}
