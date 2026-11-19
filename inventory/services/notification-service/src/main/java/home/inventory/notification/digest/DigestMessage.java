package home.inventory.notification.digest;

import home.inventory.notification.digest.dto.DueDto;
import home.inventory.notification.digest.dto.DueItemDto;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Text of the daily digest (ntfy markdown). */
public final class DigestMessage {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.");

    private DigestMessage() {
    }

    public static String title(DueDto due) {
        List<String> parts = new ArrayList<>();
        if (!due.expired().isEmpty()) {
            parts.add(due.expired().size() + " abgelaufen");
        }
        if (!due.dueToday().isEmpty()) {
            parts.add(due.dueToday().size() + " heute");
        }
        if (!due.dueSoon().isEmpty()) {
            parts.add(due.dueSoon().size() + " bald");
        }
        return "MHD: " + String.join(" · ", parts);
    }

    public static String body(DueDto due) {
        List<String> sections = new ArrayList<>();
        section(sections, "Abgelaufen", due.expired());
        section(sections, "Läuft heute ab", due.dueToday());
        section(sections, "Läuft bald ab", due.dueSoon());
        return String.join("\n\n", sections);
    }

    /** Important when something is expired or expires today. */
    public static int priority(DueDto due) {
        return due.expired().isEmpty() && due.dueToday().isEmpty() ? NtfyClient.DEFAULT_PRIORITY : NtfyClient.HIGH_PRIORITY;
    }

    private static void section(List<String> sections, String title, List<DueItemDto> items) {
        if (items.isEmpty()) {
            return;
        }
        StringBuilder text = new StringBuilder("**").append(title).append("**");
        for (DueItemDto item : items) {
            text.append("\n- ").append(item.name());
            if (item.brand() != null) {
                text.append(" (").append(item.brand()).append(')');
            }
            text.append(", ").append(item.quantity()).append("×");
            if (item.daysLeft() != 0) {
                text.append(" – ").append(when(item));
            }
        }
        sections.add(text.toString());
    }

    private static String when(DueItemDto item) {
        String date = DAY.format(item.bestBefore());
        if (item.daysLeft() < 0) {
            return "seit " + date;
        }
        return date + (item.daysLeft() == 1 ? " (morgen)" : " (in " + item.daysLeft() + " Tagen)");
    }
}
