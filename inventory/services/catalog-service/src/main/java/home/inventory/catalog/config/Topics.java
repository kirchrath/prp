package home.inventory.catalog.config;

/** Kafka topics: one topic per aggregate type, named {@code <service>_<aggregate>}. */
public final class Topics {

    public static final String CATEGORY = "catalog_category";
    public static final String ARTICLE = "catalog_article";
    public static final String STOCK_BOOKING = "stock_booking";

    /** Suffix of the retry topics of this service (one per consumed topic, e.g. {@code catalog_article_catalog_retry}). */
    public static final String RETRY_SUFFIX = "_catalog_retry";
    /** Messages that failed all attempts; only stored and logged. */
    public static final String DEAD_LETTER = "catalog_dead_letter";

    private Topics() {
    }
}
