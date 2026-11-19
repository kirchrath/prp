package home.inventory.device.scanner;

import org.jspecify.annotations.Nullable;

/** What kind of device is connected; reported by the device in its status message ({@code "kind"}). */
public enum DeviceKind {
    /** Barcode scanner with direction detection. */
    SCANNER,
    /** Button box: every button books one unit of a fixed article (short press in, long press out). */
    BUTTONS;

    /** Kind from the status message; older scanner firmware sends none. */
    public static @Nullable DeviceKind fromStatus(@Nullable String kind) {
        if (kind == null) {
            return null;
        }
        return "buttons".equalsIgnoreCase(kind) ? BUTTONS : SCANNER;
    }
}
