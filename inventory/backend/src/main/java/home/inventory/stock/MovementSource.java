package home.inventory.stock;

public enum MovementSource {
    /** ESP32 scanner via MQTT. */
    SCANNER,
    /** Camera scan in the browser. */
    MOBILE,
    /** Manual booking in the web UI. */
    UI
}
