package home.inventory.recipe.category;

import home.inventory.recipe.category.dto.CategoryDto;
import home.inventory.recipe.category.dto.CategoryListDto;
import home.inventory.recipe.category.dto.CategorySaveDto;
import home.inventory.recipe.common.ConflictException;
import home.inventory.recipe.common.EventPublisher;
import home.inventory.recipe.common.NotFoundException;
import home.inventory.recipe.common.OutgoingEvent;
import home.inventory.recipe.common.VersionCheck;
import home.inventory.recipe.config.Topics;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository repository;
    private final EventPublisher events;

    public CategoryService(CategoryRepository repository, EventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<CategoryListDto> list() {
        return repository.findAllByDeletedFalseOrderByNameAsc().stream().map(CategoryMapper::toListDto).toList();
    }

    @Transactional(readOnly = true)
    public CategoryDto get(UUID id) {
        return CategoryMapper.toDto(load(id));
    }

    /** Active category or empty (deleted/unknown). */
    @Transactional(readOnly = true)
    public Optional<Category> findActive(UUID id) {
        return repository.findById(id).filter(c -> !c.deleted());
    }

    public UUID create(CategorySaveDto request) {
        String name = request.data().name().trim();
        requireUniqueName(name, null);
        return save(Category.create(name)).id();
    }

    public void update(UUID id, CategorySaveDto request) {
        Category current = VersionCheck.require(load(id), request.audit() == null ? null : request.audit().version());
        String name = request.data().name().trim();
        requireUniqueName(name, id);
        save(current.withName(name));
    }

    public void delete(UUID id, Long version) {
        save(VersionCheck.require(load(id), version).withDeleted(true));
    }

    /** Initial categories, created once when there are none yet. */
    public static final List<String> DEFAULTS = List.of("Vorspeise", "Hauptgang", "Nachtisch", "Beilage", "Suppe",
            "Salat", "Gebäck", "Longdrink", "Aperitif", "Cocktail");

    public void seedDefaults() {
        if (repository.count() == 0) {
            DEFAULTS.forEach(name -> save(Category.create(name)));
        }
    }

    /** Active category with this name (ignoring case), used to map imported recipe categories. */
    @Transactional(readOnly = true)
    public Optional<Category> findByName(String name) {
        return repository.findByNameIgnoreCaseAndDeletedFalse(name.trim());
    }

    private Category load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Kategorie", id));
    }

    private void requireUniqueName(String name, UUID ownId) {
        repository.findByNameIgnoreCaseAndDeletedFalse(name)
                .filter(other -> !other.id().equals(ownId))
                .ifPresent(other -> {
                    throw new ConflictException("Kategorie '" + name + "' existiert bereits");
                });
    }

    private Category save(Category category) {
        Category saved = repository.save(category);
        events.publish(new OutgoingEvent(Topics.CATEGORY, saved.id(), CategoryMapper.toEventDto(saved)));
        return saved;
    }
}
