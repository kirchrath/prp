package home.inventory.stock.movement;

public enum MovementSource {
    /** ESP32 scanner (via device service). */
    SCANNER,
    /** Button box (via device service): one press = one unit of a fixed article. */
    BUTTON,
    /** Camera scan in the browser. */
    MOBILE,
    /** Manual booking in the web UI. */
    UI,
    /** Triggered by the system, e.g. merging articles. */
    SYSTEM
}
