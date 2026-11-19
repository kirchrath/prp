package home.inventory.device.firmware.dto;

import home.inventory.device.scanner.DeviceKind;
import java.time.Instant;
import java.util.UUID;

public record FirmwareListDto(UUID id, DeviceKind kind, String firmwareVersion, long size, String sha256,
                              String fileName, Instant createdDate, long version) {
}
