package home.inventory.notification.digest.dto;

import java.util.List;

/** What the daily digest contains right now. */
public record DueDto(List<DueItemDto> expired, List<DueItemDto> dueToday, List<DueItemDto> dueSoon) {

    public boolean isEmpty() {
        return expired.isEmpty() && dueToday.isEmpty() && dueSoon.isEmpty();
    }
}
