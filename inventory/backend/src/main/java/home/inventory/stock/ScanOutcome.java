package home.inventory.stock;

public enum ScanOutcome {
    /** Known article stored. */
    IN_OK,
    /** New article created from the product database and stored. */
    IN_OK_NEW_ARTICLE,
    /** Barcode unknown everywhere: placeholder created in the inbox and stored. */
    IN_OK_UNKNOWN_ARTICLE,
    /** Article removed. */
    OUT_OK,
    /** Removal of a barcode that is not known. */
    UNKNOWN_EAN,
    /** Removal although nothing is in stock at that location. */
    NOT_IN_STOCK,
    /** Scanner has no location assigned. */
    NO_LOCATION,
    /** Malformed message or unexpected error. */
    ERROR;

    public boolean isSuccess() {
        return this == IN_OK || this == IN_OK_NEW_ARTICLE || this == IN_OK_UNKNOWN_ARTICLE || this == OUT_OK;
    }
}
