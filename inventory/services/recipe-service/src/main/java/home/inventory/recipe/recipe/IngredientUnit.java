package home.inventory.recipe.recipe;

import java.math.BigDecimal;

/** Units of recipe ingredients and how they compare with package contents (GRAM, MILLILITER, PIECE). */
public enum IngredientUnit {
    GRAM(Dimension.MASS, BigDecimal.ONE),
    KILOGRAM(Dimension.MASS, BigDecimal.valueOf(1000)),
    MILLILITER(Dimension.VOLUME, BigDecimal.ONE),
    LITER(Dimension.VOLUME, BigDecimal.valueOf(1000)),
    PIECE(Dimension.COUNT, BigDecimal.ONE),
    PACKAGE(Dimension.PACKAGE, BigDecimal.ONE),
    TABLESPOON(Dimension.NONE, BigDecimal.ONE),
    TEASPOON(Dimension.NONE, BigDecimal.ONE),
    PINCH(Dimension.NONE, BigDecimal.ONE),
    OTHER(Dimension.NONE, BigDecimal.ONE);

    /** How an amount can be compared with the stock. */
    public enum Dimension {
        /** grams, compared with packages of GRAM content */
        MASS,
        /** milliliters, compared with packages of MILLILITER content */
        VOLUME,
        /** pieces, compared with packages of PIECE content (or one piece per package) */
        COUNT,
        /** whole packages */
        PACKAGE,
        /** spoons, pinches, …: available if at least one package is in stock */
        NONE
    }

    private final Dimension dimension;
    private final BigDecimal toBase;

    IngredientUnit(Dimension dimension, BigDecimal toBase) {
        this.dimension = dimension;
        this.toBase = toBase;
    }

    public Dimension dimension() {
        return dimension;
    }

    /** Factor to the base unit of the dimension (g, ml, piece). */
    public BigDecimal toBase() {
        return toBase;
    }
}
