package home.inventory.shopping.config;

/** Kafka topics: one topic per aggregate type, named {@code <service>_<aggregate>}. */
public final class Topics {

    public static final String ITEM = "shopping_item";
    public static final String CATALOG_ARTICLE = "catalog_article";
    public static final String STOCK_BATCH = "stock_batch";
    public static final String RECIPE_SHOPPING_REQUEST = "recipe_shopping_request";

    private Topics() {
    }
}
