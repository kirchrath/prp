package home.inventory.category;

import home.inventory.category.CategoryDtos.CategoryDto;
import home.inventory.category.CategoryDtos.CategoryRequest;
import home.inventory.common.NotFoundException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories")
@Transactional
public class CategoryController {

    private final CategoryRepository repository;

    CategoryController(CategoryRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<CategoryDto> listCategories() {
        return repository.findAllByOrderByNameAsc().stream().map(CategoryDto::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody CategoryRequest request) {
        return CategoryDto.of(repository.save(new Category(request.name().trim())));
    }

    @PutMapping("/{id}")
    public CategoryDto updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        Category category = repository.findById(id).orElseThrow(() -> new NotFoundException("Kategorie", id));
        category.setName(request.name().trim());
        return CategoryDto.of(category);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
