package home.inventory.device.scanner;

import home.inventory.device.scanner.dto.ScannerDto;
import home.inventory.device.scanner.dto.ScannerListDto;
import home.inventory.device.scanner.dto.ScannerSaveDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/device/scanners")
@Tag(name = "Scanners")
public class ScannerController {

    private final ScannerService service;

    ScannerController(ScannerService service) {
        this.service = service;
    }

    /** Scanners register themselves when they first connect to MQTT. */
    @GetMapping
    public List<ScannerListDto> listScanners() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ScannerDto getScanner(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateScanner(@PathVariable UUID id, @Valid @RequestBody ScannerSaveDto request) {
        service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteScanner(@PathVariable UUID id, @RequestParam Long version) {
        service.delete(id, version);
    }
}
