package home.inventory.catalog.category;

import home.inventory.catalog.category.dto.CategoryDto;
import home.inventory.catalog.category.dto.CategoryListDto;
import home.inventory.catalog.category.dto.CategorySaveDto;
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
@RequestMapping("/api/catalog/categories")
@Tag(name = "Categories")
public class CategoryController {

    private final CategoryService service;

    CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoryListDto> listCategories() {
        return service.list();
    }

    @GetMapping("/{id}")
    public CategoryDto getCategory(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<Void> createCategory(@Valid @RequestBody CategorySaveDto request) {
        return ResponseEntity.created(URI.create("/api/catalog/categories/" + service.create(request))).build();
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateCategory(@PathVariable UUID id, @Valid @RequestBody CategorySaveDto request) {
        service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable UUID id, @RequestParam Long version) {
        service.delete(id, version);
    }
}
