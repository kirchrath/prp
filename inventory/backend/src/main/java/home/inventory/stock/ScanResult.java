package home.inventory.stock;

import org.jspecify.annotations.Nullable;

public record ScanResult(ScanOutcome outcome, @Nullable Long articleId, @Nullable String articleName,
                         @Nullable Long quantityAtLocation, String message) {

    static ScanResult failure(ScanOutcome outcome, String message) {
        return new ScanResult(outcome, null, null, null, message);
    }
}
