package home.inventory.catalog.category;

import home.inventory.catalog.category.dto.CategoryDto;
import home.inventory.catalog.category.dto.CategoryListDto;
import home.inventory.catalog.category.dto.CategorySaveDto;
import home.inventory.catalog.common.ConflictException;
import home.inventory.catalog.common.EventPublisher;
import home.inventory.catalog.common.NotFoundException;
import home.inventory.catalog.common.OutgoingEvent;
import home.inventory.catalog.common.VersionCheck;
import home.inventory.catalog.config.Topics;
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
