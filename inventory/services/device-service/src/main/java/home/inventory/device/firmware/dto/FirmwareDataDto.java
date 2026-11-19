package home.inventory.device.firmware.dto;

import home.inventory.device.scanner.DeviceKind;

public record FirmwareDataDto(DeviceKind kind, String firmwareVersion, long size, String sha256, String fileName) {
}
