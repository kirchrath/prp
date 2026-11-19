package home.inventory.notification.settings.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;

/**
 * @param enabled             daily digest on/off
 * @param sendTime            local time of the daily digest ("08:00")
 * @param defaultDays         "expiring soon" this many days before the best-before date
 * @param categoryThresholds  other thresholds for single article categories
 */
public record SettingsDataDto(boolean enabled, @NotNull LocalTime sendTime, @Min(0) @Max(365) int defaultDays,
                              @NotNull List<@NotNull @Valid CategoryThresholdDto> categoryThresholds) {
}
