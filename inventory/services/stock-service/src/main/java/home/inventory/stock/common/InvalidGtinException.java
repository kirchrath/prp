package home.inventory.stock.common;

/** A barcode is not a formally valid GTIN (see {@link Gtin}). */
public class InvalidGtinException extends RuntimeException {

    public InvalidGtinException(String code) {
        super(Gtin.INVALID + " (" + code.trim() + ")");
    }
}
