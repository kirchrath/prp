package home.inventory.scanner;

import home.inventory.scanner.ScannerDtos.ScannerDto;
import home.inventory.scanner.ScannerDtos.ScannerUpdateRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scanners")
@Tag(name = "Scanners")
@Transactional
public class ScannerController {

    private final ScannerService service;

    ScannerController(ScannerService service) {
        this.service = service;
    }

    /** Scanners register themselves when they first connect to MQTT. */
    @GetMapping
    @Transactional(readOnly = true)
    public List<ScannerDto> listScanners() {
        return service.findAll().stream().map(ScannerDto::of).toList();
    }

    @PutMapping("/{id}")
    public ScannerDto updateScanner(@PathVariable Long id, @Valid @RequestBody ScannerUpdateRequest request) {
        return ScannerDto.of(service.update(id, request.name(), request.locationId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteScanner(@PathVariable Long id) {
        service.delete(id);
    }
}
