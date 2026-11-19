package home.inventory.stock.common;

import java.util.Objects;
import org.springframework.dao.OptimisticLockingFailureException;

public final class VersionCheck {

    private VersionCheck() {
    }

    /** Fails if the client edited an outdated state of the aggregate. */
    public static <T extends AuditedEntity<T>> T require(T current, Long expectedVersion) {
        if (!Objects.equals(current.version(), expectedVersion)) {
            throw new OptimisticLockingFailureException(
                    "Der Datensatz wurde zwischenzeitlich geändert (Version " + current.version() + ")");
        }
        return current;
    }
}
