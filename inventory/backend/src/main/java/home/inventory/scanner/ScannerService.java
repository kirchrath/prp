package home.inventory.scanner;

import home.inventory.common.NotFoundException;
import home.inventory.location.LocationService;
import home.inventory.location.StorageLocation;
import home.inventory.scanner.ScannerMessages.ScanMessage;
import home.inventory.scanner.ScannerMessages.StatusMessage;
import home.inventory.stock.Booking;
import home.inventory.stock.ScanDirection;
import home.inventory.stock.ScanOutcome;
import home.inventory.stock.ScanResult;
import home.inventory.stock.ScanService;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ScannerService {

    private final ScannerRepository scanners;
    private final LocationService locations;
    private final ScanService scans;

    ScannerService(ScannerRepository scanners, LocationService locations, ScanService scans) {
        this.scanners = scanners;
        this.locations = locations;
        this.scans = scans;
    }

    @Transactional(readOnly = true)
    public List<Scanner> findAll() {
        return scanners.findAllOrdered();
    }

    public Scanner update(Long id, @Nullable String name, @Nullable Long locationId) {
        Scanner scanner = scanners.findById(id).orElseThrow(() -> new NotFoundException("Scanner", id));
        scanner.setName(name == null || name.isBlank() ? null : name.trim());
        scanner.setLocation(locationId == null ? null : locations.get(locationId));
        return scanner;
    }

    public void delete(Long id) {
        scanners.deleteById(id);
    }

    public void handleStatus(String deviceId, StatusMessage status) {
        Scanner scanner = register(deviceId);
        scanner.setOnline(status.online());
        if (status.online()) {
            scanner.setFirmwareVersion(status.fw());
            scanner.setIpAddress(status.ip());
        }
    }

    /** Processes a scan; returns empty for duplicates (QoS 1 redelivery), which must not be answered again. */
    public Optional<ScanResult> handleScan(String deviceId, ScanMessage message) {
        Scanner scanner = register(deviceId);
        scanner.setOnline(true);
        if (scanner.isDuplicate(message.bootId(), message.seq())) {
            return Optional.empty();
        }
        scanner.remember(message.bootId(), message.seq());

        ScanDirection direction = parseDirection(message.direction());
        if (message.ean() == null || message.ean().isBlank() || direction == null) {
            return Optional.of(new ScanResult(ScanOutcome.ERROR, null, null, null, "Ungültige Scan-Nachricht"));
        }
        StorageLocation location = scanner.getLocation();
        if (location == null) {
            return Optional.of(new ScanResult(ScanOutcome.NO_LOCATION, null, null, null,
                    "Scanner " + scanner.getDisplayName() + " ist keinem Lagerort zugeordnet"));
        }
        return Optional.of(scans.scan(message.ean().trim(), location, direction, Booking.scanner(scanner)));
    }

    private Scanner register(String deviceId) {
        Scanner scanner = scanners.findByDeviceId(deviceId).orElseGet(() -> scanners.save(new Scanner(deviceId)));
        scanner.setLastSeen(Instant.now());
        return scanner;
    }

    private static @Nullable ScanDirection parseDirection(@Nullable String value) {
        if (value == null) {
            return null;
        }
        try {
            return ScanDirection.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
