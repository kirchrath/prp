package home.inventory.notification.settings;

import home.inventory.notification.common.AuditMapper;
import home.inventory.notification.common.ConflictException;
import home.inventory.notification.common.EventPublisher;
import home.inventory.notification.common.OutgoingEvent;
import home.inventory.notification.common.VersionCheck;
import home.inventory.notification.config.Topics;
import home.inventory.notification.settings.dto.CategoryThresholdDto;
import home.inventory.notification.settings.dto.SettingsDataDto;
import home.inventory.notification.settings.dto.SettingsDto;
import home.inventory.notification.settings.dto.SettingsEventDto;
import home.inventory.notification.settings.dto.SettingsSaveDto;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SettingsService {

    private final NotificationSettingsRepository repository;
    private final EventPublisher events;

    public SettingsService(NotificationSettingsRepository repository, EventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public NotificationSettings current() {
        return repository.findById(NotificationSettings.ID).orElseThrow();
    }

    @Transactional(readOnly = true)
    public SettingsDto get() {
        NotificationSettings s = current();
        return new SettingsDto(AuditMapper.toDto(s), data(s));
    }

    public void update(SettingsSaveDto request) {
        NotificationSettings current = VersionCheck.require(current(), request.audit().version());
        SettingsDataDto data = request.data();
        Set<CategoryThreshold> thresholds = new HashSet<>();
        Set<UUID> categories = new HashSet<>();
        for (CategoryThresholdDto t : data.categoryThresholds()) {
            if (!categories.add(t.categoryId())) {
                throw new ConflictException("Für eine Kategorie ist nur eine Schwelle möglich");
            }
            thresholds.add(new CategoryThreshold(t.categoryId(), t.days()));
        }
        NotificationSettings saved = repository.save(current.withData(data.enabled(),
                data.sendTime().truncatedTo(ChronoUnit.MINUTES), data.defaultDays(), thresholds));
        events.publish(new OutgoingEvent(Topics.SETTINGS, saved.id(),
                new SettingsEventDto(saved.id(), AuditMapper.toDto(saved), data(saved))));
    }

    private static SettingsDataDto data(NotificationSettings s) {
        return new SettingsDataDto(s.enabled(), s.sendTime(), s.defaultDays(), s.categoryThresholds().stream()
                .sorted(Comparator.comparing(t -> t.categoryId().toString()))
                .map(t -> new CategoryThresholdDto(t.categoryId(), t.days()))
                .toList());
    }
}
