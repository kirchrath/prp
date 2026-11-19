package home.inventory.recipe.category;

import home.inventory.recipe.category.dto.CategoryDto;
import home.inventory.recipe.category.dto.CategoryListDto;
import home.inventory.recipe.category.dto.CategorySaveDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/recipe/categories")
@Tag(name = "RecipeCategories")
public class CategoryController {

    private final CategoryService service;

    CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoryListDto> listRecipeCategories() {
        return service.list();
    }

    @GetMapping("/{id}")
    public CategoryDto getRecipeCategory(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<Void> createRecipeCategory(@Valid @RequestBody CategorySaveDto request) {
        return ResponseEntity.created(URI.create("/api/recipe/categories/" + service.create(request))).build();
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRecipeCategory(@PathVariable UUID id, @Valid @RequestBody CategorySaveDto request) {
        service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecipeCategory(@PathVariable UUID id, @RequestParam Long version) {
        service.delete(id, version);
    }
}
