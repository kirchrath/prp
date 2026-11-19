package home.inventory.stock.location;

import home.inventory.stock.location.dto.LocationDto;
import home.inventory.stock.location.dto.LocationListDto;
import home.inventory.stock.location.dto.LocationSaveDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock/locations")
@Tag(name = "Locations")
public class LocationController {

    private final LocationService service;

    LocationController(LocationService service) {
        this.service = service;
    }

    @GetMapping
    public List<LocationListDto> listLocations() {
        return service.list();
    }

    @GetMapping("/{id}")
    public LocationDto getLocation(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<Void> createLocation(@Valid @RequestBody LocationSaveDto request) {
        return ResponseEntity.created(URI.create("/api/stock/locations/" + service.create(request))).build();
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateLocation(@PathVariable UUID id, @Valid @RequestBody LocationSaveDto request) {
        service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLocation(@PathVariable UUID id, @RequestParam Long version) {
        service.delete(id, version);
    }
}
