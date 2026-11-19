package home.inventory.notification.digest;

import home.inventory.notification.common.AuditMapper;
import home.inventory.notification.common.EventPublisher;
import home.inventory.notification.common.OutgoingEvent;
import home.inventory.notification.config.NotificationProperties;
import home.inventory.notification.config.Topics;
import home.inventory.notification.digest.dto.DigestDataDto;
import home.inventory.notification.digest.dto.DigestEventDto;
import home.inventory.notification.digest.dto.DigestListDto;
import home.inventory.notification.digest.dto.DueDto;
import home.inventory.notification.settings.NotificationSettings;
import home.inventory.notification.settings.SettingsService;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Sends the daily best-before digest: once per day from the configured time, only if something is due.
 * If ntfy cannot be reached, it is tried again after {@code retry-after}, at most {@code max-attempts} times.
 */
@Service
public class DigestService {

    private static final Logger log = LoggerFactory.getLogger(DigestService.class);

    private final DigestRepository repository;
    private final DueService due;
    private final SettingsService settings;
    private final NtfyClient ntfy;
    private final NotificationProperties properties;
    private final EventPublisher events;

    public DigestService(DigestRepository repository, DueService due, SettingsService settings, NtfyClient ntfy,
                         NotificationProperties properties, EventPublisher events) {
        this.repository = repository;
        this.due = due;
        this.settings = settings;
        this.ntfy = ntfy;
        this.properties = properties;
        this.events = events;
    }

    /** Called every minute by the scheduler. */
    public void runIfDue(ZonedDateTime now) {
        NotificationSettings config = settings.current();
        if (!config.enabled() || now.toLocalTime().isBefore(config.sendTime())) {
            return;
        }
        Optional<Digest> existing = repository.findByDay(now.toLocalDate());
        if (existing.isPresent() && !retryDue(existing.get(), now)) {
            return;
        }
        Digest digest = existing.orElseGet(() -> Digest.create(now.toLocalDate()));
        DueDto items = due.due(now.toLocalDate());
        int expired = items.expired().size();
        int today = items.dueToday().size();
        int soon = items.dueSoon().size();
        if (items.isEmpty()) {
            save(digest.withResult(DigestStatus.NOTHING_DUE, 0, 0, 0, null));
            return;
        }
        try {
            ntfy.send(DigestMessage.title(items), DigestMessage.body(items), DigestMessage.priority(items),
                    List.of("hourglass"));
            save(digest.withResult(DigestStatus.SENT, expired, today, soon, null));
        } catch (NotificationFailedException e) {
            log.warn("MHD-Zusammenfassung konnte nicht gesendet werden: {}", e.getMessage());
            save(digest.withResult(DigestStatus.FAILED, expired, today, soon, e.getMessage()));
        }
    }

    private boolean retryDue(Digest digest, ZonedDateTime now) {
        return digest.status() == DigestStatus.FAILED && digest.attempts() < properties.maxAttempts()
                && !digest.lastModifiedDate().plus(properties.retryAfter()).isAfter(now.toInstant());
    }

    public void sendTest() {
        ntfy.send("Testnachricht", "Benachrichtigungen vom Vorrat kommen an.", NtfyClient.DEFAULT_PRIORITY,
                List.of("white_check_mark"));
    }

    public List<DigestListDto> recent() {
        return repository.findTop14ByDeletedFalseOrderByDayDesc().stream()
                .map(d -> new DigestListDto(d.id(), d.day(), d.status(), d.attempts(), d.expired(), d.dueToday(),
                        d.dueSoon(), d.error(), d.lastModifiedDate()))
                .toList();
    }

    private void save(Digest digest) {
        Digest saved = repository.save(digest);
        events.publish(new OutgoingEvent(Topics.DIGEST, saved.id(), new DigestEventDto(saved.id(),
                AuditMapper.toDto(saved), new DigestDataDto(saved.day(), saved.status(), saved.attempts(),
                saved.expired(), saved.dueToday(), saved.dueSoon(), saved.error()))));
    }
}
