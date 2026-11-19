package home.inventory.location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public final class LocationDtos {

    private LocationDtos() {
    }

    public record LocationDto(Long id, String name, @Nullable String description, @Nullable Long parentId,
                              String path) {
        public static LocationDto of(StorageLocation l) {
            StorageLocation parent = l.getParent();
            return new LocationDto(l.getId(), l.getName(), l.getDescription(),
                    parent == null ? null : parent.getId(), l.getPath());
        }
    }

    public record LocationRequest(@NotBlank @Size(max = 100) String name,
                                  @Nullable @Size(max = 500) String description,
                                  @Nullable Long parentId) {
    }
}
