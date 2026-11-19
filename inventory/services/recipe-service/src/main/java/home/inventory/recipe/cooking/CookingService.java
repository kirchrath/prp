package home.inventory.recipe.cooking;

import home.inventory.recipe.cooking.dto.MissingIngredientDto;
import home.inventory.recipe.cooking.dto.RecipeSuggestionDto;
import home.inventory.recipe.copy.ArticleCopy;
import home.inventory.recipe.copy.CopyService;
import home.inventory.recipe.recipe.IngredientUnit;
import home.inventory.recipe.recipe.IngredientUnit.Dimension;
import home.inventory.recipe.recipe.Recipe;
import home.inventory.recipe.recipe.RecipeIngredient;
import home.inventory.recipe.recipe.RecipeService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "What can I cook for x persons?": compares the scaled ingredients with the stock. Available amount =
 * packages in stock × content of one package; only whole packages are counted.
 */
@Service
@Transactional(readOnly = true)
public class CookingService {

    private final RecipeService recipes;
    private final CopyService copies;

    public CookingService(RecipeService recipes, CopyService copies) {
        this.recipes = recipes;
        this.copies = copies;
    }

    /** All matching recipes: cookable ones first, then by number of missing ingredients. */
    public List<RecipeSuggestionDto> suggestions(int servings, @Nullable UUID categoryId, @Nullable String tag) {
        Stock stock = stock();
        Map<UUID, String> categories = recipes.categoryNames();
        return recipes.active(null, categoryId, tag).stream()
                .map(r -> {
                    List<MissingIngredientDto> missing = missing(r, servings, stock);
                    return new RecipeSuggestionDto(r.id(), r.title(), r.imageUrl(),
                            r.categoryId() == null ? null : categories.get(r.categoryId()), r.totalMinutes(),
                            missing.isEmpty(), missing);
                })
                .sorted(Comparator.comparingInt((RecipeSuggestionDto s) -> s.missing().size())
                        .thenComparing(RecipeSuggestionDto::title, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Missing ingredients of one recipe for the given servings. */
    public List<MissingIngredientDto> missing(Recipe recipe, int servings) {
        return missing(recipe, servings, stock());
    }

    record Stock(Map<UUID, ArticleCopy> articles, Map<UUID, Long> packages) {
    }

    private Stock stock() {
        return new Stock(copies.activeArticles().stream().collect(Collectors.toMap(ArticleCopy::articleId, Function.identity())),
                copies.packages());
    }

    private static List<MissingIngredientDto> missing(Recipe recipe, int servings, Stock stock) {
        List<MissingIngredientDto> result = new ArrayList<>();
        for (RecipeIngredient ingredient : recipe.ingredients()) {
            BigDecimal amount = ingredient.amount() == null ? null : ingredient.amount()
                    .multiply(BigDecimal.valueOf(servings))
                    .divide(BigDecimal.valueOf(recipe.servings()), 2, RoundingMode.HALF_UP)
                    .stripTrailingZeros();
            int packages = packagesToBuy(ingredient, amount, stock);
            if (packages > 0) {
                ArticleCopy article = ingredient.articleId() == null ? null : stock.articles().get(ingredient.articleId());
                result.add(new MissingIngredientDto(ingredient.name(), amount, ingredient.unit(), ingredient.articleId(),
                        article == null ? null : article.name(), packages));
            }
        }
        return result;
    }

    /** 0 if the ingredient is available, otherwise the packages to buy (1 if no package size is known). */
    static int packagesToBuy(RecipeIngredient ingredient, @Nullable BigDecimal amount, Stock stock) {
        if (ingredient.basic()) {
            return 0;
        }
        ArticleCopy article = ingredient.articleId() == null ? null : stock.articles().get(ingredient.articleId());
        if (article == null) {
            return 1;
        }
        long have = stock.packages().getOrDefault(article.articleId(), 0L);
        IngredientUnit unit = ingredient.unit();
        BigDecimal perPackage = perPackage(unit, article);
        if (amount == null || perPackage == null) {
            return have > 0 ? 0 : 1;
        }
        BigDecimal missing = amount.multiply(unit.toBase()).subtract(perPackage.multiply(BigDecimal.valueOf(have)));
        return missing.signum() <= 0 ? 0 : missing.divide(perPackage, 0, RoundingMode.CEILING).intValueExact();
    }

    /** Content of one package in the base unit of the ingredient's dimension, or null if not comparable. */
    private static @Nullable BigDecimal perPackage(@Nullable IngredientUnit unit, ArticleCopy article) {
        Dimension dimension = unit == null ? Dimension.NONE : unit.dimension();
        String content = article.contentUnit();
        return switch (dimension) {
            case PACKAGE -> BigDecimal.ONE;
            case COUNT -> "PIECE".equals(content) ? article.contentAmount() : content == null ? BigDecimal.ONE : null;
            case MASS -> "GRAM".equals(content) ? article.contentAmount() : null;
            case VOLUME -> "MILLILITER".equals(content) ? article.contentAmount() : null;
            case NONE -> null;
        };
    }
}
