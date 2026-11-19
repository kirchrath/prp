package home.inventory.stock.movement;

public enum MovementSource {
    /** ESP32 scanner (via device service). */
    SCANNER,
    /** Camera scan in the browser. */
    MOBILE,
    /** Manual booking in the web UI. */
    UI,
    /** Triggered by the system, e.g. merging articles. */
    SYSTEM
}
