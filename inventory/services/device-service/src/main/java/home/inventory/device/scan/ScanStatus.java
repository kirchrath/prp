package home.inventory.device.scan;

public enum ScanStatus {
    /** Sent to the stock service, waiting for the booking. */
    REQUESTED,
    /** The stock service booked it (successfully or not, see {@code result}). */
    PROCESSED,
    /** Not forwarded, e.g. because the scanner has no location. */
    REJECTED
}
