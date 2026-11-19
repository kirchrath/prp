package home.inventory.stock.location;

import home.inventory.stock.common.AuditMapper;
import home.inventory.stock.common.ConflictException;
import home.inventory.stock.common.EventPublisher;
import home.inventory.stock.common.NotFoundException;
import home.inventory.stock.common.OutgoingEvent;
import home.inventory.stock.common.VersionCheck;
import home.inventory.stock.config.Topics;
import home.inventory.stock.batch.StockBatchRepository;
import home.inventory.stock.location.dto.LocationDataDto;
import home.inventory.stock.location.dto.LocationDto;
import home.inventory.stock.location.dto.LocationEventDto;
import home.inventory.stock.location.dto.LocationListDto;
import home.inventory.stock.location.dto.LocationSaveDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LocationService {

    private final LocationRepository repository;
    private final StockBatchRepository batches;
    private final EventPublisher events;

    public LocationService(LocationRepository repository, StockBatchRepository batches, EventPublisher events) {
        this.repository = repository;
        this.batches = batches;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<LocationListDto> list() {
        Map<UUID, String> paths = paths();
        return repository.findAllByDeletedFalse().stream()
                .map(l -> new LocationListDto(l.id(), l.name(), l.description(), l.parentId(), paths.get(l.id())))
                .sorted(Comparator.comparing(LocationListDto::path, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public LocationDto get(UUID id) {
        Location l = load(id);
        return new LocationDto(AuditMapper.toDto(l), data(l));
    }

    /** Active location or empty. */
    @Transactional(readOnly = true)
    public Optional<Location> findActive(UUID id) {
        return repository.findById(id).filter(l -> !l.deleted());
    }

    /** Full path ("Keller / Regal 2") of every location, deleted ones included (needed for history). */
    @Transactional(readOnly = true)
    public Map<UUID, String> paths() {
        Map<UUID, Location> all = repository.findAll().stream().collect(Collectors.toMap(Location::id, Function.identity()));
        return all.values().stream().collect(Collectors.toMap(Location::id, l -> path(l, all)));
    }

    /** Id of the location and all of its active descendants. */
    @Transactional(readOnly = true)
    public Set<UUID> subtree(UUID rootId) {
        List<Location> active = repository.findAllByDeletedFalse();
        Set<UUID> result = new HashSet<>(Set.of(rootId));
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Location l : active) {
                if (l.parentId() != null && result.contains(l.parentId()) && result.add(l.id())) {
                    changed = true;
                }
            }
        }
        return result;
    }

    public UUID create(LocationSaveDto request) {
        LocationDataDto data = request.data();
        UUID parentId = requireParent(data.parentId(), null);
        return save(Location.create(data.name().trim(), blankToNull(data.description()), parentId)).id();
    }

    public void update(UUID id, LocationSaveDto request) {
        Location current = VersionCheck.require(load(id), request.audit() == null ? null : request.audit().version());
        LocationDataDto data = request.data();
        UUID parentId = requireParent(data.parentId(), id);
        save(current.withData(data.name().trim(), blankToNull(data.description()), parentId));
    }

    public void delete(UUID id, Long version) {
        Location location = VersionCheck.require(load(id), version);
        if (repository.existsByParentIdAndDeletedFalse(id)) {
            throw new ConflictException("Lagerort enthält noch Unter-Lagerorte");
        }
        if (batches.existsByLocationIdAndDeletedFalse(id)) {
            throw new ConflictException("Lagerort enthält noch Bestand");
        }
        save(location.withDeleted(true));
    }

    private @Nullable UUID requireParent(@Nullable UUID parentId, @Nullable UUID ownId) {
        if (parentId == null) {
            return null;
        }
        if (findActive(parentId).isEmpty()) {
            throw new NotFoundException("Lagerort", parentId);
        }
        if (ownId != null && subtree(ownId).contains(parentId)) {
            throw new ConflictException("Ein Lagerort kann nicht unter sich selbst verschoben werden");
        }
        return parentId;
    }

    private static String path(Location location, Map<UUID, Location> all) {
        List<String> parts = new ArrayList<>();
        for (Location l = location; l != null; l = l.parentId() == null ? null : all.get(l.parentId())) {
            parts.addFirst(l.name());
        }
        return String.join(" / ", parts);
    }

    private Location load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Lagerort", id));
    }

    private Location save(Location location) {
        Location saved = repository.save(location);
        events.publish(new OutgoingEvent(Topics.LOCATION, saved.id(),
                new LocationEventDto(saved.id(), AuditMapper.toDto(saved), data(saved))));
        return saved;
    }

    private static LocationDataDto data(Location l) {
        return new LocationDataDto(l.name(), l.description(), l.parentId());
    }

    private static @Nullable String blankToNull(@Nullable String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
