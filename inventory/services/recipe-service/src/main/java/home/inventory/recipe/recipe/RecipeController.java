package home.inventory.recipe.recipe;

import home.inventory.recipe.recipe.dto.RecipeDto;
import home.inventory.recipe.recipe.dto.RecipeListDto;
import home.inventory.recipe.recipe.dto.RecipeSaveDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipe/recipes")
@Tag(name = "Recipes")
public class RecipeController {

    private final RecipeService service;

    RecipeController(RecipeService service) {
        this.service = service;
    }

    @GetMapping
    public List<RecipeListDto> listRecipes(@RequestParam(required = false) @Nullable String search,
                                           @RequestParam(required = false) @Nullable UUID categoryId,
                                           @RequestParam(required = false) @Nullable String tag) {
        return service.list(search, categoryId, tag);
    }

    @GetMapping("/tags")
    public List<String> listRecipeTags() {
        return service.tags();
    }

    @GetMapping("/{id}")
    public RecipeDto getRecipe(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<Void> createRecipe(@Valid @RequestBody RecipeSaveDto request) {
        return ResponseEntity.created(URI.create("/api/recipe/recipes/" + service.create(request))).build();
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRecipe(@PathVariable UUID id, @Valid @RequestBody RecipeSaveDto request) {
        service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecipe(@PathVariable UUID id, @RequestParam Long version) {
        service.delete(id, version);
    }
}
