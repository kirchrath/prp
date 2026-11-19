package home.inventory.device;

import static org.assertj.core.api.Assertions.assertThat;

import home.inventory.device.common.UuidGeneratingCallback;
import home.inventory.device.scanner.Scanner;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidGeneratingCallbackTest {

    private final UuidGeneratingCallback callback = new UuidGeneratingCallback();

    @Test
    void assignsIdToNewAggregates() {
        assertThat(((Scanner) callback.onBeforeConvert(Scanner.create("x"))).id()).isNotNull();
    }

    @Test
    void keepsExistingId() {
        Scanner scanner = Scanner.create("x").withId(UUID.randomUUID());
        assertThat(callback.onBeforeConvert(scanner)).isSameAs(scanner);
    }

    @Test
    void duplicateDetection() {
        Scanner fresh = Scanner.create("x");
        assertThat(fresh.isDuplicate("b", 1)).isFalse();
        Scanner seen = fresh.withLastScan("b", 5, Instant.now());
        assertThat(seen.isDuplicate("b", 5)).isTrue();
        assertThat(seen.isDuplicate("b", 6)).isFalse();
        assertThat(seen.isDuplicate("other", 1)).isFalse();
        Scanner noSeq = new Scanner(null, null, null, null, false, "x", null, null, true, null, null, null, "b", null,
                home.inventory.device.scanner.DeviceKind.SCANNER, java.util.Set.of());
        assertThat(noSeq.isDuplicate("b", 1)).isFalse();
        assertThat(fresh.displayName()).isEqualTo("x");
        assertThat(fresh.withAssignment("Küche", null, java.util.Set.of()).displayName()).isEqualTo("Küche");
    }
}
