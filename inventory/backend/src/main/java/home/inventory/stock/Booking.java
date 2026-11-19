package home.inventory.stock;

import home.inventory.common.CurrentUser;
import home.inventory.scanner.Scanner;
import org.jspecify.annotations.Nullable;

/** Who/what triggered a stock change; stored on every {@link StockMovement}. */
public record Booking(MovementSource source, @Nullable Scanner scanner, @Nullable String username) {

    public static Booking ui() {
        return new Booking(MovementSource.UI, null, CurrentUser.name());
    }

    public static Booking mobile() {
        return new Booking(MovementSource.MOBILE, null, CurrentUser.name());
    }

    public static Booking scanner(Scanner scanner) {
        return new Booking(MovementSource.SCANNER, scanner, null);
    }
}
