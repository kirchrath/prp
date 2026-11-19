package home.inventory.stock.batch;

import home.inventory.stock.common.ConflictException;

public class InsufficientStockException extends ConflictException {

    public InsufficientStockException(int available) {
        super("Nicht genug Bestand (vorhanden: " + available + ")");
    }
}
