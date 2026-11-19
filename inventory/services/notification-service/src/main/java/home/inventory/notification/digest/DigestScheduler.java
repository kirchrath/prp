package home.inventory.notification.digest;

import java.time.Clock;
import java.time.ZonedDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Checks every minute whether the daily digest is due ({@code inventory.notification.cron}, "-" = off). */
@Component
public class DigestScheduler {

    private final DigestService digests;
    private final Clock clock;

    public DigestScheduler(DigestService digests, Clock clock) {
        this.digests = digests;
        this.clock = clock;
    }

    @Scheduled(cron = "${inventory.notification.cron:0 * * * * *}")
    public void tick() {
        digests.runIfDue(ZonedDateTime.now(clock));
    }
}
