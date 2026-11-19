package home.inventory.location;

import home.inventory.location.LocationDtos.LocationDto;
import home.inventory.location.LocationDtos.LocationRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/locations")
@Tag(name = "Locations")
@Transactional
public class LocationController {

    private final LocationService service;

    LocationController(LocationService service) {
        this.service = service;
    }

    /** All locations as a flat list (sorted by path); the tree is built via {@code parentId}. */
    @GetMapping
    @Transactional(readOnly = true)
    public List<LocationDto> listLocations() {
        return service.findAll().stream().map(LocationDto::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocationDto createLocation(@Valid @RequestBody LocationRequest request) {
        return LocationDto.of(service.create(request));
    }

    @PutMapping("/{id}")
    public LocationDto updateLocation(@PathVariable Long id, @Valid @RequestBody LocationRequest request) {
        return LocationDto.of(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLocation(@PathVariable Long id) {
        service.delete(id);
    }
}
