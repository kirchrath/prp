package home.inventory.stock.common;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {
    }

    /** Name of the authenticated user or {@code null} (e.g. when processing Kafka events). */
    public static @Nullable String name() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }
}
