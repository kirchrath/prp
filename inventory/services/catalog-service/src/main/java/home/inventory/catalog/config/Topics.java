package home.inventory.catalog.config;

/** Kafka topics: one topic per aggregate type, named {@code <service>_<aggregate>}. */
public final class Topics {

    public static final String CATEGORY = "catalog_category";
    public static final String ARTICLE = "catalog_article";
    public static final String STOCK_BOOKING = "stock_booking";

    private Topics() {
    }
}
