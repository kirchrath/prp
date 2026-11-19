package home.inventory.recipe.cooking;

import static org.assertj.core.api.Assertions.assertThat;

import home.inventory.recipe.copy.ArticleCopy;
import home.inventory.recipe.recipe.IngredientUnit;
import home.inventory.recipe.recipe.RecipeIngredient;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PackagesToBuyTest {

    private static final UUID PASTA = UUID.randomUUID();     // 500 g per package
    private static final UUID MILK = UUID.randomUUID();      // 1000 ml
    private static final UUID EGGS = UUID.randomUUID();      // 10 pieces
    private static final UUID LEMON = UUID.randomUUID();     // no content
    private static final UUID SPICE = UUID.randomUUID();     // 50 g, none in stock

    private static ArticleCopy article(UUID id, String amount, String unit) {
        return new ArticleCopy(UUID.randomUUID(), 0L, null, null, false, id, 0, "A", null,
                amount == null ? null : new BigDecimal(amount), unit);
    }

    private final CookingService.Stock stock = new CookingService.Stock(
            Map.of(PASTA, article(PASTA, "500", "GRAM"), MILK, article(MILK, "1000", "MILLILITER"),
                    EGGS, article(EGGS, "10", "PIECE"), LEMON, article(LEMON, null, null), SPICE, article(SPICE, "50", "GRAM")),
            Map.of(PASTA, 1L, MILK, 2L, EGGS, 1L, LEMON, 2L));

    private static int buy(CookingService.Stock stock, UUID article, String amount, IngredientUnit unit, boolean basic) {
        BigDecimal value = amount == null ? null : new BigDecimal(amount);
        return CookingService.packagesToBuy(new RecipeIngredient("x", value, unit, article, basic), value, stock);
    }

    private int buy(UUID article, String amount, IngredientUnit unit) {
        return buy(stock, article, amount, unit, false);
    }

    @Test
    void comparesAmountsWithPackageContent() {
        assertThat(buy(PASTA, "500", IngredientUnit.GRAM)).isZero();
        assertThat(buy(PASTA, "501", IngredientUnit.GRAM)).isEqualTo(1);
        assertThat(buy(PASTA, "1.2", IngredientUnit.KILOGRAM)).isEqualTo(2);
        assertThat(buy(MILK, "2", IngredientUnit.LITER)).isZero();
        assertThat(buy(MILK, "2500", IngredientUnit.MILLILITER)).isEqualTo(1);
        assertThat(buy(EGGS, "12", IngredientUnit.PIECE)).isEqualTo(1);
        assertThat(buy(LEMON, "2", IngredientUnit.PIECE)).isZero();      // one piece per package
        assertThat(buy(LEMON, "3", IngredientUnit.PIECE)).isEqualTo(1);
        assertThat(buy(PASTA, "2", IngredientUnit.PACKAGE)).isEqualTo(1);
        assertThat(buy(SPICE, "1", IngredientUnit.TEASPOON)).isEqualTo(1); // presence only, none in stock
    }

    @Test
    void fallsBackToPresenceWhenNotComparable() {
        assertThat(buy(PASTA, "2", IngredientUnit.TABLESPOON)).isZero();
        assertThat(buy(PASTA, "200", IngredientUnit.MILLILITER)).isZero();   // grams vs. milliliters
        assertThat(buy(MILK, "200", IngredientUnit.GRAM)).isZero();
        assertThat(buy(PASTA, "2", IngredientUnit.PIECE)).isZero();          // pieces of a GRAM article
        assertThat(buy(PASTA, null, null)).isZero();
        assertThat(buy(PASTA, "3", null)).isZero();
        assertThat(buy(SPICE, null, null)).isEqualTo(1);
    }

    @Test
    void basicUnknownAndMissingArticles() {
        assertThat(buy(stock, SPICE, "100", IngredientUnit.GRAM, true)).isZero();
        assertThat(buy(stock, null, "100", IngredientUnit.GRAM, false)).isEqualTo(1);
        assertThat(buy(stock, UUID.randomUUID(), "100", IngredientUnit.GRAM, false)).isEqualTo(1);
        assertThat(buy(SPICE, "120", IngredientUnit.GRAM)).isEqualTo(3);
    }
}
