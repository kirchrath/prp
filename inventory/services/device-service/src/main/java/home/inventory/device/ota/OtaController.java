package home.inventory.device.ota;

import home.inventory.device.ota.dto.OtaRolloutDto;
import home.inventory.device.scanner.DeviceKind;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/device")
@Tag(name = "FirmwareUpdates")
public class OtaController {

    private final OtaService service;

    OtaController(OtaService service) {
        this.service = service;
    }

    @PostMapping("/scanners/{id}/firmware-update")
    @Operation(summary = "Installs firmware on one device (default: newest of its kind); 409 if offline")
    public ResponseEntity<Void> startFirmwareUpdate(@PathVariable UUID id,
                                                    @RequestParam(required = false) @Nullable UUID firmwareId) {
        UUID job = service.start(id, firmwareId);
        return ResponseEntity.accepted().location(URI.create("/api/device/ota-jobs/" + job)).build();
    }

    @PostMapping("/firmware-updates")
    @Operation(summary = "Installs the newest firmware on all online devices of a kind that need it")
    public OtaRolloutDto startFirmwareRollout(@RequestParam DeviceKind kind) {
        return service.startAll(kind);
    }
}
