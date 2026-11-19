package home.inventory.recipe.config;

/** Kafka topics: one topic per aggregate type, named {@code <service>_<aggregate>}. */
public final class Topics {

    public static final String CATEGORY = "recipe_category";
    public static final String RECIPE = "recipe_recipe";
    public static final String SHOPPING_REQUEST = "recipe_shopping_request";
    public static final String CATALOG_ARTICLE = "catalog_article";
    public static final String STOCK_BATCH = "stock_batch";

    /** Suffix of the retry topics of this service (one per consumed topic, e.g. {@code catalog_article_recipe_retry}). */
    public static final String RETRY_SUFFIX = "_recipe_retry";
    /** Messages that failed all attempts; only stored and logged. */
    public static final String DEAD_LETTER = "recipe_dead_letter";

    private Topics() {
    }
}
