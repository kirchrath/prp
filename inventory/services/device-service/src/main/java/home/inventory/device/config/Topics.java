package home.inventory.device.config;

/** Kafka topics: one topic per aggregate type, named {@code <service>_<aggregate>}. */
public final class Topics {

    public static final String SCANNER = "device_scanner";
    public static final String SCAN = "device_scan";
    public static final String STOCK_BOOKING = "stock_booking";

    /** Suffix of the retry topics of this service (one per consumed topic, e.g. {@code catalog_article_device_retry}). */
    public static final String RETRY_SUFFIX = "_device_retry";
    /** Messages that failed all attempts; only stored and logged. */
    public static final String DEAD_LETTER = "device_dead_letter";

    private Topics() {
    }
}
