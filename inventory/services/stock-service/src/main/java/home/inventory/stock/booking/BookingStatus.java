package home.inventory.stock.booking;

public enum BookingStatus {
    /** Just received, not processed yet. */
    REQUESTED,
    /** Barcode unknown: waits until the catalog service has created the article. */
    PENDING_ARTICLE,
    /** Known article stored. */
    IN_OK,
    /** Article was created by the catalog from a product database, then stored. */
    IN_OK_NEW_ARTICLE,
    /** Article unknown everywhere: inbox placeholder created, then stored. */
    IN_OK_UNKNOWN_ARTICLE,
    OUT_OK,
    /** Removal of a barcode that is not known. */
    UNKNOWN_EAN,
    /** Removal although nothing is in stock at that location. */
    NOT_IN_STOCK,
    /** Location missing or deleted. */
    NO_LOCATION
}
