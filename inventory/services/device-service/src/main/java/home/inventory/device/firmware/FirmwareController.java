package home.inventory.device.firmware;

import home.inventory.device.firmware.dto.FirmwareListDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/device/firmware")
@Tag(name = "Firmware")
public class FirmwareController {

    private final FirmwareService service;

    FirmwareController(FirmwareService service) {
        this.service = service;
    }

    @GetMapping
    public List<FirmwareListDto> listFirmware() {
        return service.list();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploads a firmware image (firmware.bin); kind and version are read from the image")
    public ResponseEntity<Void> uploadFirmware(@RequestPart("file") MultipartFile file) throws IOException {
        UUID id = service.upload(file.getBytes(), file.getOriginalFilename() == null ? "firmware.bin" : file.getOriginalFilename());
        return ResponseEntity.created(URI.create("/api/device/firmware/" + id)).build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFirmware(@PathVariable UUID id, @RequestParam Long version) {
        service.delete(id, version);
    }
}
