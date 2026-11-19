package home.inventory.recipe.shopping;

import home.inventory.recipe.common.AuditMapper;
import home.inventory.recipe.common.ConflictException;
import home.inventory.recipe.common.EventPublisher;
import home.inventory.recipe.common.NotFoundException;
import home.inventory.recipe.common.OutgoingEvent;
import home.inventory.recipe.config.Topics;
import home.inventory.recipe.cooking.CookingService;
import home.inventory.recipe.cooking.dto.MissingIngredientDto;
import home.inventory.recipe.recipe.IngredientUnit;
import home.inventory.recipe.recipe.Recipe;
import home.inventory.recipe.recipe.RecipeService;
import home.inventory.recipe.shopping.dto.ShoppingRequestDataDto;
import home.inventory.recipe.shopping.dto.ShoppingRequestDto;
import home.inventory.recipe.shopping.dto.ShoppingRequestEventDto;
import home.inventory.recipe.shopping.dto.ShoppingRequestItemDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Puts the missing ingredients of a recipe onto the shopping list (via topic {@code recipe_shopping_request}). */
@Service
@Transactional
public class ShoppingRequestService {

    static final Map<IngredientUnit, String> UNIT_LABELS = Map.of(
            IngredientUnit.GRAM, "g", IngredientUnit.KILOGRAM, "kg", IngredientUnit.MILLILITER, "ml",
            IngredientUnit.LITER, "l", IngredientUnit.PIECE, "Stück", IngredientUnit.PACKAGE, "Packung",
            IngredientUnit.TABLESPOON, "EL", IngredientUnit.TEASPOON, "TL", IngredientUnit.PINCH, "Prise",
            IngredientUnit.OTHER, "");

    private final ShoppingRequestRepository repository;
    private final RecipeService recipes;
    private final CookingService cooking;
    private final EventPublisher events;

    public ShoppingRequestService(ShoppingRequestRepository repository, RecipeService recipes, CookingService cooking,
                                  EventPublisher events) {
        this.repository = repository;
        this.recipes = recipes;
        this.cooking = cooking;
        this.events = events;
    }

    public UUID create(UUID recipeId, int servings) {
        Recipe recipe = recipes.load(recipeId);
        List<ShoppingRequestItem> items = cooking.missing(recipe, servings).stream().map(ShoppingRequestService::item).toList();
        if (items.isEmpty()) {
            throw new ConflictException("Für " + recipe.title() + " fehlt nichts");
        }
        ShoppingRequest saved = repository.save(ShoppingRequest.create(recipe.id(), recipe.title(), servings, items));
        events.publish(new OutgoingEvent(Topics.SHOPPING_REQUEST, saved.id(),
                new ShoppingRequestEventDto(saved.id(), AuditMapper.toDto(saved), data(saved))));
        return saved.id();
    }

    @Transactional(readOnly = true)
    public ShoppingRequestDto get(UUID id) {
        ShoppingRequest r = repository.findById(id).orElseThrow(() -> new NotFoundException("Einkaufsanfrage", id));
        return new ShoppingRequestDto(AuditMapper.toDto(r), data(r));
    }

    /** Articles by name with the number of packages; ingredients without article as text with the amount. */
    static ShoppingRequestItem item(MissingIngredientDto m) {
        if (m.articleId() != null && m.articleName() != null) {
            return new ShoppingRequestItem(m.articleId(), m.articleName(), m.packages());
        }
        String amount = m.amount() == null ? "" : " (" + (m.amount().toPlainString() + " "
                + UNIT_LABELS.getOrDefault(m.unit(), "")).trim() + ")";
        return new ShoppingRequestItem(null, m.name() + amount, 1);
    }

    private static ShoppingRequestDataDto data(ShoppingRequest r) {
        return new ShoppingRequestDataDto(r.recipeId(), r.recipeTitle(), r.servings(),
                r.items().stream().map(i -> new ShoppingRequestItemDto(i.articleId(), i.text(), i.quantity())).toList());
    }
}
