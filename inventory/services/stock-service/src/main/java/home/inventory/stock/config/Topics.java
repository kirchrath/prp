package home.inventory.stock.config;

/** Kafka topics: one topic per aggregate type, named {@code <service>_<aggregate>}. */
public final class Topics {

    public static final String LOCATION = "stock_location";
    public static final String BATCH = "stock_batch";
    public static final String MOVEMENT = "stock_movement";
    public static final String BOOKING = "stock_booking";
    public static final String CATALOG_ARTICLE = "catalog_article";
    public static final String DEVICE_SCAN = "device_scan";

    private Topics() {
    }
}
