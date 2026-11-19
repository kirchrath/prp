package home.inventory.location;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

@Entity
public class StorageLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private @Nullable String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private @Nullable StorageLocation parent;

    protected StorageLocation() {
    }

    public StorageLocation(String name, @Nullable String description, @Nullable StorageLocation parent) {
        this.name = name;
        this.description = description;
        this.parent = parent;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public @Nullable String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public @Nullable StorageLocation getParent() {
        return parent;
    }

    public void setParent(@Nullable StorageLocation parent) {
        this.parent = parent;
    }

    /** Full path such as "Keller / Regal 2 / oben". */
    public String getPath() {
        List<String> parts = new ArrayList<>();
        // use getters: the parent may be an uninitialized Hibernate proxy
        for (StorageLocation l = this; l != null; l = l.getParent()) {
            parts.addFirst(l.getName());
        }
        return String.join(" / ", parts);
    }

    public boolean isSelfOrAncestorOf(StorageLocation other) {
        for (StorageLocation l = other; l != null; l = l.getParent()) {
            if (l.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }
}
