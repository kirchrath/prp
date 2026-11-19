package home.inventory.recipe.imports;

import home.inventory.recipe.category.Category;
import home.inventory.recipe.category.CategoryService;
import home.inventory.recipe.category.dto.CategoryListDto;
import home.inventory.recipe.config.RecipeProperties;
import home.inventory.recipe.copy.ArticleCopy;
import home.inventory.recipe.copy.CopyService;
import home.inventory.recipe.recipe.dto.IngredientDto;
import home.inventory.recipe.recipe.dto.RecipeDataDto;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Creates recipe drafts (not saved) from a link or a free text. Recipe pages are read from their schema.org data
 * where available; otherwise, and for free texts, the AI extracts the recipe. The AI also suggests the matching
 * articles. The user reviews the draft and saves it.
 */
@Service
public class ImportService {

    static final int DEFAULT_SERVINGS = 4;

    private final PageFetcher fetcher;
    private final JsonLdRecipeParser jsonLd;
    private final RecipeAiService ai;
    private final CategoryService categories;
    private final CopyService copies;
    private final RecipeProperties properties;

    public ImportService(PageFetcher fetcher, JsonLdRecipeParser jsonLd, RecipeAiService ai, CategoryService categories,
                         CopyService copies, RecipeProperties properties) {
        this.fetcher = fetcher;
        this.jsonLd = jsonLd;
        this.ai = ai;
        this.categories = categories;
        this.copies = copies;
        this.properties = properties;
    }

    public RecipeDataDto fromText(String text) {
        List<ArticleCopy> articles = articles();
        return draft(ai.extractRecipe(text.trim(), categoryNames(), articles), null, null, articles);
    }

    public RecipeDataDto fromUrl(String url) {
        String html = fetcher.fetch(URI.create(url.trim()));
        List<ArticleCopy> articles = articles();
        Optional<JsonLdRecipe> structured = jsonLd.parse(html);
        if (structured.isPresent()) {
            JsonLdRecipe r = structured.get();
            List<AiIngredient> ingredients = r.ingredients().isEmpty() ? List.of()
                    : ai.structureIngredients(r.ingredients(), articles);
            return draft(new AiRecipe(r.title(), r.description(), r.servings(), r.prepMinutes(), r.cookMinutes(),
                    r.category(), r.tags(), r.steps(), ingredients), url.trim(), r.imageUrl(), articles);
        }
        String text = PageFetcher.text(html, properties.maxPageText());
        return draft(ai.extractRecipe(text, categoryNames(), articles), url.trim(), PageFetcher.previewImage(html), articles);
    }

    private RecipeDataDto draft(AiRecipe r, @Nullable String sourceUrl, @Nullable String imageUrl,
                                List<ArticleCopy> articles) {
        Set<String> articleIds = articles.stream().map(a -> a.articleId().toString()).collect(Collectors.toSet());
        List<IngredientDto> ingredients = orEmpty(r.ingredients()).stream()
                .filter(i -> i.name() != null && !i.name().isBlank())
                .map(i -> new IngredientDto(i.name().trim(),
                        i.amount() != null && i.amount().signum() > 0 ? i.amount() : null,
                        i.amount() != null && i.amount().signum() > 0 ? i.unit() : null,
                        i.articleId() != null && articleIds.contains(i.articleId()) ? UUID.fromString(i.articleId()) : null,
                        i.basic()))
                .toList();
        UUID categoryId = r.category() == null ? null : categories.findByName(r.category()).map(Category::id).orElse(null);
        return new RecipeDataDto(
                r.title() == null || r.title().isBlank() ? "Neues Rezept" : r.title().trim(),
                r.description(),
                r.servings() == null || r.servings() < 1 ? DEFAULT_SERVINGS : Math.min(r.servings(), 100),
                minutes(r.prepMinutes()),
                minutes(r.cookMinutes()),
                imageUrl == null || imageUrl.isBlank() ? null : imageUrl,
                sourceUrl,
                categoryId,
                orEmpty(r.tags()).stream().filter(t -> t != null && !t.isBlank())
                        .map(t -> t.trim().toLowerCase(Locale.ROOT)).distinct().toList(),
                orEmpty(r.steps()).stream().filter(s -> s != null && !s.isBlank()).map(String::trim).toList(),
                ingredients);
    }

    private List<ArticleCopy> articles() {
        return copies.activeArticles().stream().limit(properties.maxArticles()).toList();
    }

    private List<String> categoryNames() {
        return categories.list().stream().map(CategoryListDto::name).toList();
    }

    private static @Nullable Integer minutes(@Nullable Integer value) {
        return value == null || value < 0 ? null : value;
    }

    private static <T> List<T> orEmpty(@Nullable List<T> list) {
        return list == null ? List.of() : list;
    }
}
