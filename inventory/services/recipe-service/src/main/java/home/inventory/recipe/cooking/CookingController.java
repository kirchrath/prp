package home.inventory.recipe.cooking;

import home.inventory.recipe.cooking.dto.RecipeSuggestionDto;
import home.inventory.recipe.shopping.ShoppingRequestService;
import home.inventory.recipe.shopping.dto.ShoppingRequestDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipe")
@Tag(name = "Cooking")
@Validated
public class CookingController {

    private final CookingService cooking;
    private final ShoppingRequestService shopping;

    CookingController(CookingService cooking, ShoppingRequestService shopping) {
        this.cooking = cooking;
        this.shopping = shopping;
    }

    /** What can I cook for {@code servings} persons? */
    @GetMapping("/suggestions")
    public List<RecipeSuggestionDto> listSuggestions(@RequestParam @Min(1) @Max(100) int servings,
                                                     @RequestParam(required = false) @Nullable UUID categoryId,
                                                     @RequestParam(required = false) @Nullable String tag) {
        return cooking.suggestions(servings, categoryId, tag);
    }

    /** Puts the missing ingredients for {@code servings} persons onto the shopping list. */
    @PostMapping("/recipes/{id}/shopping-requests")
    public ResponseEntity<Void> createShoppingRequest(@PathVariable UUID id, @RequestParam @Min(1) @Max(100) int servings) {
        return ResponseEntity.created(URI.create("/api/recipe/shopping-requests/" + shopping.create(id, servings))).build();
    }

    @GetMapping("/shopping-requests/{id}")
    public ShoppingRequestDto getShoppingRequest(@PathVariable UUID id) {
        return shopping.get(id);
    }
}
