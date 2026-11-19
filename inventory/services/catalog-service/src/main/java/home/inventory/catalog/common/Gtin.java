package home.inventory.catalog.common;

import java.util.Optional;

/**
 * Formal check of article numbers (GTIN family): only digits, EAN-8, UPC-A (12), EAN-13 or GTIN-14 with a valid
 * check digit. UPC-E (8 digits, compressed UPC-A) is accepted when it is not a valid EAN-8.
 * <p>
 * Normalized form, so that the same product always has the same code: UPC-A and UPC-E become EAN-13 (leading 0),
 * a GTIN-14 with a leading 0 becomes EAN-13, EAN-8 stays EAN-8.
 */
public final class Gtin {

    /** Message for invalid codes, shown to the user. */
    public static final String INVALID = "Ungültige EAN: erlaubt sind 8, 12, 13 oder 14 Ziffern mit gültiger Prüfziffer";

    private Gtin() {
    }

    /** Normalized code, or empty when the code is not a formally valid GTIN. */
    public static Optional<String> normalize(String raw) {
        String code = raw.trim();
        if (!code.matches("\\d+")) {
            return Optional.empty();
        }
        return switch (code.length()) {
            case 8 -> hasValidCheckDigit(code) ? Optional.of(code) : upcE(code);
            case 12 -> checked("0" + code);
            case 13 -> checked(code);
            case 14 -> checked(code.charAt(0) == '0' ? code.substring(1) : code);
            default -> Optional.empty();
        };
    }

    /** Normalized code; throws {@link InvalidGtinException} when the code is not a formally valid GTIN. */
    public static String require(String raw) {
        return normalize(raw).orElseThrow(() -> new InvalidGtinException(raw));
    }

    private static Optional<String> checked(String code) {
        return hasValidCheckDigit(code) ? Optional.of(code) : Optional.empty();
    }

    /** UPC-E (number system 0 or 1) expanded to UPC-A, as EAN-13. */
    private static Optional<String> upcE(String code) {
        char system = code.charAt(0);
        if (system != '0' && system != '1') {
            return Optional.empty();
        }
        String d = code.substring(1, 7);
        char last = d.charAt(5);
        String body = switch (last) {
            case '0', '1', '2' -> d.substring(0, 2) + last + "0000" + d.substring(2, 5);
            case '3' -> d.substring(0, 3) + "00000" + d.substring(3, 5);
            case '4' -> d.substring(0, 4) + "00000" + d.charAt(4);
            default -> d.substring(0, 5) + "0000" + last;
        };
        return checked("0" + system + body + code.charAt(7));
    }

    /** GS1 check digit: weights 3 and 1 alternating from the right, excluding the check digit itself. */
    static boolean hasValidCheckDigit(String code) {
        int sum = 0;
        for (int i = code.length() - 2, weight = 3; i >= 0; i--, weight = 4 - weight) {
            sum += (code.charAt(i) - '0') * weight;
        }
        return (10 - sum % 10) % 10 == code.charAt(code.length() - 1) - '0';
    }
}
