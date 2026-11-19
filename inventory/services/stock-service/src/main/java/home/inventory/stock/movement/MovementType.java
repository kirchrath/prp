package home.inventory.stock.movement;

public enum MovementType {
    IN,
    OUT,
    /** Manual correction of a batch quantity (inventory count). */
    CORRECTION,
    /** Stock moved to another article because articles were merged. */
    MERGE
}
