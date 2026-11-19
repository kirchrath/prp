package home.inventory.shopping.recipe;

import home.inventory.shopping.item.ShoppingItemService;
import home.inventory.shopping.recipe.dto.ShoppingRequestEventDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Puts the missing ingredients of a recipe onto the shopping list, each request only once. */
@Service
@Transactional
public class RecipeRequestService {

    private final ProcessedRequestRepository processed;
    private final ShoppingItemService items;

    public RecipeRequestService(ProcessedRequestRepository processed, ShoppingItemService items) {
        this.processed = processed;
        this.items = items;
    }

    public void handle(ShoppingRequestEventDto event) {
        if (event.audit().deleted() || processed.existsByRequestId(event.id())) {
            return;
        }
        processed.save(ProcessedRequest.create(event.id()));
        String reason = "für " + event.data().recipeTitle() + " (" + event.data().servings() + " Pers.)";
        event.data().items().forEach(i -> items.addFromRecipe(i.articleId(), i.text(), i.quantity(), reason));
    }
}
