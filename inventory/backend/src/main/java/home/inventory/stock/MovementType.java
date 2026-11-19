package home.inventory.stock;

public enum MovementType {
    IN,
    OUT,
    /** Manual correction of a batch quantity (inventory count). */
    CORRECTION
}
