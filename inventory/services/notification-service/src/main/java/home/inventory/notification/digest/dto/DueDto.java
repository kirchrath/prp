package home.inventory.notification.digest.dto;

import java.util.List;

/** What the daily digest contains right now. */
public record DueDto(List<DueItemDto> expired, List<DueItemDto> dueToday, List<DueItemDto> dueSoon) {

    /** No getter (no "is" prefix), so it is not part of the JSON. */
    public boolean nothingDue() {
        return expired.isEmpty() && dueToday.isEmpty() && dueSoon.isEmpty();
    }
}
