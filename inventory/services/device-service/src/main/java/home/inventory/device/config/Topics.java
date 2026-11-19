package home.inventory.device.config;

/** Kafka topics: one topic per aggregate type, named {@code <service>_<aggregate>}. */
public final class Topics {

    public static final String SCANNER = "device_scanner";
    public static final String SCAN = "device_scan";
    public static final String STOCK_BOOKING = "stock_booking";

    private Topics() {
    }
}
