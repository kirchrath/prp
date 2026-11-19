package home.inventory.recipe.imports;

import home.inventory.recipe.imports.dto.TextImportDto;
import home.inventory.recipe.imports.dto.UrlImportDto;
import home.inventory.recipe.recipe.dto.RecipeDataDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Creates recipe drafts for review; nothing is saved. */
@RestController
@RequestMapping("/api/recipe/imports")
@Tag(name = "RecipeImports")
public class ImportController {

    private final ImportService service;

    ImportController(ImportService service) {
        this.service = service;
    }

    @PostMapping("/url")
    public RecipeDataDto importRecipeFromUrl(@Valid @RequestBody UrlImportDto request) {
        return service.fromUrl(request.url());
    }

    @PostMapping("/text")
    public RecipeDataDto importRecipeFromText(@Valid @RequestBody TextImportDto request) {
        return service.fromText(request.text());
    }
}
