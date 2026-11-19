package home.inventory.catalog.article;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Reads the package content from texts like "500 g", "1,5 l", "6 x 0,33 l", "10 Stück" (as delivered by
 * Open Food Facts or typed by the user) and converts it into a base unit.
 */
public final class PackageContentParser {

    private static final Pattern PATTERN = Pattern.compile(
            "(?:(\\d+)\\s*[x×*]\\s*)?(\\d+(?:\\.\\d+)?)\\s*(kg|mg|gr|g|ml|cl|dl|l|stück|stk|st|pcs|pc)(?![a-zäöü])");

    private record Unit(ContentUnit unit, BigDecimal factor) {
    }

    private static final Map<String, Unit> UNITS = Map.ofEntries(
            Map.entry("kg", new Unit(ContentUnit.GRAM, BigDecimal.valueOf(1000))),
            Map.entry("g", new Unit(ContentUnit.GRAM, BigDecimal.ONE)),
            Map.entry("gr", new Unit(ContentUnit.GRAM, BigDecimal.ONE)),
            Map.entry("mg", new Unit(ContentUnit.GRAM, new BigDecimal("0.001"))),
            Map.entry("l", new Unit(ContentUnit.MILLILITER, BigDecimal.valueOf(1000))),
            Map.entry("dl", new Unit(ContentUnit.MILLILITER, BigDecimal.valueOf(100))),
            Map.entry("cl", new Unit(ContentUnit.MILLILITER, BigDecimal.TEN)),
            Map.entry("ml", new Unit(ContentUnit.MILLILITER, BigDecimal.ONE)),
            Map.entry("stück", new Unit(ContentUnit.PIECE, BigDecimal.ONE)),
            Map.entry("stk", new Unit(ContentUnit.PIECE, BigDecimal.ONE)),
            Map.entry("st", new Unit(ContentUnit.PIECE, BigDecimal.ONE)),
            Map.entry("pcs", new Unit(ContentUnit.PIECE, BigDecimal.ONE)),
            Map.entry("pc", new Unit(ContentUnit.PIECE, BigDecimal.ONE)));

    private PackageContentParser() {
    }

    public static Optional<PackageContent> parse(@Nullable String text) {
        if (text == null) {
            return Optional.empty();
        }
        Matcher m = PATTERN.matcher(text.toLowerCase(Locale.GERMAN).replace(',', '.'));
        if (!m.find()) {
            return Optional.empty();
        }
        BigDecimal count = m.group(1) == null ? BigDecimal.ONE : new BigDecimal(m.group(1));
        Unit unit = UNITS.get(m.group(3));
        BigDecimal amount = new BigDecimal(m.group(2)).multiply(count).multiply(unit.factor()).stripTrailingZeros();
        return amount.signum() > 0 ? Optional.of(new PackageContent(amount, unit.unit())) : Optional.empty();
    }
}
