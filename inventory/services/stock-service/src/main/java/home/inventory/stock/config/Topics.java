package home.inventory.stock.config;

/** Kafka topics: one topic per aggregate type, named {@code <service>_<aggregate>}. */
public final class Topics {

    public static final String LOCATION = "stock_location";
    public static final String BATCH = "stock_batch";
    public static final String MOVEMENT = "stock_movement";
    public static final String BOOKING = "stock_booking";
    public static final String CATALOG_ARTICLE = "catalog_article";
    public static final String DEVICE_SCAN = "device_scan";

    /** Suffix of the retry topics of this service (one per consumed topic, e.g. {@code catalog_article_stock_retry}). */
    public static final String RETRY_SUFFIX = "_stock_retry";
    /** Messages that failed all attempts; only stored and logged. */
    public static final String DEAD_LETTER = "stock_dead_letter";

    private Topics() {
    }
}
