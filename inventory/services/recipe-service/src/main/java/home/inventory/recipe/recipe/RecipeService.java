package home.inventory.recipe.recipe;

import home.inventory.recipe.category.Category;
import home.inventory.recipe.category.CategoryRepository;
import home.inventory.recipe.category.CategoryService;
import home.inventory.recipe.common.EventPublisher;
import home.inventory.recipe.common.NotFoundException;
import home.inventory.recipe.common.OutgoingEvent;
import home.inventory.recipe.common.VersionCheck;
import home.inventory.recipe.config.Topics;
import home.inventory.recipe.recipe.dto.IngredientDto;
import home.inventory.recipe.recipe.dto.RecipeDataDto;
import home.inventory.recipe.recipe.dto.RecipeDto;
import home.inventory.recipe.recipe.dto.RecipeListDto;
import home.inventory.recipe.recipe.dto.RecipeSaveDto;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RecipeService {

    private final RecipeRepository repository;
    private final CategoryService categories;
    private final CategoryRepository categoryRepository;
    private final EventPublisher events;

    public RecipeService(RecipeRepository repository, CategoryService categories, CategoryRepository categoryRepository,
                         EventPublisher events) {
        this.repository = repository;
        this.categories = categories;
        this.categoryRepository = categoryRepository;
        this.events = events;
    }

    /** Active recipes filtered by text (title, description, ingredients), category and tag. */
    @Transactional(readOnly = true)
    public List<RecipeListDto> list(@Nullable String search, @Nullable UUID categoryId, @Nullable String tag) {
        Map<UUID, String> names = categoryNames();
        return active(search, categoryId, tag).stream()
                .map(r -> RecipeMapper.toListDto(r, r.categoryId() == null ? null : names.get(r.categoryId())))
                .toList();
    }

    /** Active recipes matching the filters, sorted by title. */
    @Transactional(readOnly = true)
    public List<Recipe> active(@Nullable String search, @Nullable UUID categoryId, @Nullable String tag) {
        String term = normalize(search);
        String wantedTag = normalize(tag);
        return repository.findAllByDeletedFalse().stream()
                .filter(r -> categoryId == null || categoryId.equals(r.categoryId()))
                .filter(r -> wantedTag == null || r.tags().contains(new RecipeTag(wantedTag)))
                .filter(r -> term == null || matches(r, term))
                .sorted(Comparator.comparing(Recipe::title, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** All tags in use, for the filter. */
    @Transactional(readOnly = true)
    public List<String> tags() {
        return repository.findAllByDeletedFalse().stream()
                .flatMap(r -> r.tags().stream().map(RecipeTag::name))
                .distinct().sorted().toList();
    }

    @Transactional(readOnly = true)
    public RecipeDto get(UUID id) {
        return RecipeMapper.toDto(load(id));
    }

    @Transactional(readOnly = true)
    public Recipe load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Rezept", id));
    }

    public UUID create(RecipeSaveDto request) {
        return save(apply(Recipe.create(), request.data())).id();
    }

    public void update(UUID id, RecipeSaveDto request) {
        Recipe current = VersionCheck.require(load(id), request.audit() == null ? null : request.audit().version());
        save(apply(current, request.data()));
    }

    public void delete(UUID id, Long version) {
        save(VersionCheck.require(load(id), version).withDeleted(true));
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> categoryNames() {
        return categoryRepository.findAll().stream().collect(Collectors.toMap(Category::id, Category::name));
    }

    private Recipe apply(Recipe recipe, RecipeDataDto data) {
        UUID categoryId = data.categoryId();
        if (categoryId != null && categories.findActive(categoryId).isEmpty()) {
            throw new NotFoundException("Kategorie", categoryId);
        }
        Set<RecipeTag> tags = data.tags().stream().map(RecipeService::normalize)
                .map(RecipeTag::new).collect(Collectors.toCollection(LinkedHashSet::new));
        List<RecipeStep> steps = data.steps().stream().map(s -> new RecipeStep(s.trim())).toList();
        List<RecipeIngredient> ingredients = data.ingredients().stream().map(RecipeService::ingredient).toList();
        return recipe.withData(data.title().trim(), blankToNull(data.description()), data.servings(), data.prepMinutes(),
                data.cookMinutes(), blankToNull(data.imageUrl()), blankToNull(data.sourceUrl()), categoryId, tags, steps,
                ingredients);
    }

    private static RecipeIngredient ingredient(IngredientDto i) {
        return new RecipeIngredient(i.name().trim(), i.amount() == null ? null : i.amount().stripTrailingZeros(),
                i.unit(), i.articleId(), i.basic());
    }

    private static boolean matches(Recipe r, String term) {
        return r.title().toLowerCase(Locale.ROOT).contains(term)
                || (r.description() != null && r.description().toLowerCase(Locale.ROOT).contains(term))
                || r.ingredients().stream().anyMatch(i -> i.name().toLowerCase(Locale.ROOT).contains(term));
    }

    private Recipe save(Recipe recipe) {
        Recipe saved = repository.save(recipe);
        events.publish(new OutgoingEvent(Topics.RECIPE, saved.id(), RecipeMapper.toEventDto(saved)));
        return saved;
    }

    private static @Nullable String normalize(@Nullable String s) {
        return s == null || s.isBlank() ? null : s.trim().toLowerCase(Locale.ROOT);
    }

    private static @Nullable String blankToNull(@Nullable String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
