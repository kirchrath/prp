package home.inventory.notification.config;

/** Kafka topics: one topic per aggregate type, named {@code <service>_<aggregate>}. */
public final class Topics {

    public static final String SETTINGS = "notification_settings";
    public static final String DIGEST = "notification_digest";
    public static final String CATALOG_ARTICLE = "catalog_article";
    public static final String STOCK_BATCH = "stock_batch";

    /** Suffix of the retry topics of this service (one per consumed topic, e.g. {@code catalog_article_notification_retry}). */
    public static final String RETRY_SUFFIX = "_notification_retry";
    /** Messages that failed all attempts; only stored and logged. */
    public static final String DEAD_LETTER = "notification_dead_letter";

    private Topics() {
    }
}
