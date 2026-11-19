package home.inventory.shopping.config;

/** Kafka topics: one topic per aggregate type, named {@code <service>_<aggregate>}. */
public final class Topics {

    public static final String ITEM = "shopping_item";
    public static final String CATALOG_ARTICLE = "catalog_article";
    public static final String STOCK_BATCH = "stock_batch";
    public static final String RECIPE_SHOPPING_REQUEST = "recipe_shopping_request";

    /** Suffix of the retry topics of this service (one per consumed topic, e.g. {@code catalog_article_shopping_retry}). */
    public static final String RETRY_SUFFIX = "_shopping_retry";
    /** Messages that failed all attempts; only stored and logged. */
    public static final String DEAD_LETTER = "shopping_dead_letter";

    private Topics() {
    }
}
