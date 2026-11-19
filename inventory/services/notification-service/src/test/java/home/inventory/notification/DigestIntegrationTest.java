package home.inventory.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import home.inventory.notification.digest.DigestRepository;
import home.inventory.notification.digest.DigestScheduler;
import home.inventory.notification.digest.DigestService;
import home.inventory.notification.digest.DigestStatus;
import home.inventory.notification.digest.DueService;
import home.inventory.notification.digest.dto.DueDto;
import home.inventory.notification.digest.dto.DueItemDto;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class DigestIntegrationTest extends AbstractIntegrationTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final LocalDate DAY = LocalDate.of(2030, 3, 10);

    @Autowired
    DueService due;

    @Autowired
    DigestService digests;

    @Autowired
    DigestRepository repository;

    @Autowired
    DigestScheduler scheduler;

    @Autowired
    ObjectMapper json;

    private static ZonedDateTime at(LocalDate day, int hour, int minute) {
        return day.atTime(hour, minute).atZone(ZONE);
    }

    private void touched(ZonedDateTime time) {
        jdbc.update("update digest set last_modified_date = ? where day = ?",
                java.sql.Timestamp.from(time.toInstant()), DAY);
    }

    private void thresholds(String categoryThresholds) {
        jdbc.update("update notification_settings set default_days = 3");
        for (String part : categoryThresholds.split(";")) {
            String[] kv = part.split("=");
            jdbc.update("insert into category_threshold (settings_id, category_id, days) values (?, ?, ?)",
                    UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString(kv[0]), Integer.parseInt(kv[1]));
        }
    }

    @Test
    void dueUsesCategoryThresholdsAndGroupsBatches() {
        UUID dairy = UUID.randomUUID();
        UUID cans = UUID.randomUUID();
        thresholds(dairy + "=1;" + cans + "=30");

        UUID milk = article("Milch", "Weide", dairy);
        UUID beans = article("Bohnen", null, cans);
        UUID rice = article("Reis", null, null);
        UUID gone = article("Weg", null, null);
        article(gone, 2, true, "Weg", null, null);

        batch(milk, DAY.minusDays(2), 1);          // expired
        batch(milk, DAY.minusDays(2), 2);          // same date, other location: summed
        batch(milk, DAY.plusDays(1), 1);           // dairy: 1 day -> soon
        batch(milk, DAY.plusDays(2), 1);           // dairy: 2 days -> not yet
        batch(beans, DAY.plusDays(20), 3);         // cans: 30 days -> soon
        batch(rice, DAY, 1);                       // today
        batch(rice, DAY.plusDays(3), 1);           // default 3 days -> soon
        batch(rice, DAY.plusDays(4), 1);           // not yet
        batch(gone, DAY, 1);                       // deleted article: ignored
        batch(UUID.randomUUID(), DAY, 1);          // unknown article: ignored
        UUID empty = batch(rice, DAY.minusDays(1), 1);
        batch(empty, 2, false, rice, DAY.minusDays(1), 0); // used up
        batch(empty, 1, false, rice, DAY.minusDays(1), 5); // older version: ignored
        batch(rice, null, 4);                      // no best-before date

        DueDto result = due.due(DAY);
        assertThat(result.expired()).extracting(DueItemDto::name, DueItemDto::quantity, DueItemDto::daysLeft)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Milch", 3, -2L));
        assertThat(result.dueToday()).extracting(DueItemDto::name).containsExactly("Reis");
        assertThat(result.dueSoon()).extracting(DueItemDto::name, DueItemDto::daysLeft).containsExactly(
                org.assertj.core.groups.Tuple.tuple("Milch", 1L),
                org.assertj.core.groups.Tuple.tuple("Reis", 3L),
                org.assertj.core.groups.Tuple.tuple("Bohnen", 20L));
        assertThat(result.isEmpty()).isFalse();

        // article updates: older versions are ignored
        article(milk, 0, false, "Alt", null, dairy);
        assertThat(due.due(DAY).expired().getFirst().name()).isEqualTo("Milch");
    }

    @Test
    void dailyDigestIsSentOnceFromTheConfiguredTime() throws Exception {
        UUID rice = article("Reis", "Uncle", null);
        batch(rice, DAY.minusDays(1), 2);
        batch(rice, DAY, 1);
        batch(rice, DAY.plusDays(1), 1);
        batch(rice, DAY.plusDays(2), 1);

        digests.runIfDue(at(DAY, 7, 59));
        assertThat(FakeNtfy.requests()).isEmpty();

        digests.runIfDue(at(DAY, 8, 0));
        assertThat(FakeNtfy.requests()).hasSize(1);
        JsonNode message = json.readTree(FakeNtfy.requests().getFirst().body());
        assertThat(message.get("title").asString()).isEqualTo("MHD: 1 abgelaufen · 1 heute · 2 bald");
        assertThat(message.get("message").asString()).isEqualTo("""
                **Abgelaufen**
                - Reis (Uncle), 2× – seit 09.03.

                **Läuft heute ab**
                - Reis (Uncle), 1×

                **Läuft bald ab**
                - Reis (Uncle), 1× – 11.03. (morgen)
                - Reis (Uncle), 1× – 12.03. (in 2 Tagen)""");
        assertThat(message.get("priority").asInt()).isEqualTo(4);
        assertThat(message.get("markdown").asBoolean()).isTrue();
        assertThat(FakeNtfy.requests().getFirst().authorization()).isEqualTo("Bearer secret");

        // once per day
        digests.runIfDue(at(DAY, 9, 0));
        assertThat(FakeNtfy.requests()).hasSize(1);
        var digest = repository.findByDay(DAY).orElseThrow();
        assertThat(digest.status()).isEqualTo(DigestStatus.SENT);
        assertThat(digest.expired()).isEqualTo(1);
        assertThat(kafka.await("notification_digest", digest.id().toString(), 1)).isNotEmpty();

        mvc.perform(get("/api/notification/digests").with(USER))
                .andExpect(jsonPath("$[0].day").value("2030-03-10"))
                .andExpect(jsonPath("$[0].status").value("SENT"));
    }

    @Test
    void nothingDueSendsNothingAndOnlySoonIsNormalPriority() throws Exception {
        digests.runIfDue(at(DAY, 8, 0));
        assertThat(FakeNtfy.requests()).isEmpty();
        assertThat(repository.findByDay(DAY).orElseThrow().status()).isEqualTo(DigestStatus.NOTHING_DUE);

        UUID rice = article("Reis", null, null);
        batch(rice, DAY.plusDays(3), 1);
        LocalDate next = DAY.plusDays(1);
        digests.runIfDue(at(next, 8, 0));
        JsonNode message = json.readTree(FakeNtfy.requests().getFirst().body());
        assertThat(message.get("title").asString()).isEqualTo("MHD: 1 bald");
        assertThat(message.get("message").asString()).isEqualTo("**Läuft bald ab**\n- Reis, 1× – 13.03. (in 2 Tagen)");
        assertThat(message.get("priority").asInt()).isEqualTo(3);

        // disabled: nothing at all
        jdbc.update("update notification_settings set enabled = false");
        digests.runIfDue(at(DAY.plusDays(2), 9, 0));
        assertThat(repository.findByDay(DAY.plusDays(2))).isEmpty();
    }

    @Test
    void failedDigestIsRetriedLater() {
        UUID rice = article("Reis", null, null);
        batch(rice, DAY, 1);
        FakeNtfy.failWith(503);

        ZonedDateTime first = at(DAY, 8, 0);
        digests.runIfDue(first);
        var failed = repository.findByDay(DAY).orElseThrow();
        assertThat(failed.status()).isEqualTo(DigestStatus.FAILED);
        assertThat(failed.attempts()).isEqualTo(1);
        assertThat(failed.error()).startsWith("ntfy:");

        // the last attempt is pinned to the simulated time (auditing uses the real clock)
        touched(first);
        digests.runIfDue(first.plusMinutes(14));
        assertThat(repository.findByDay(DAY).orElseThrow().attempts()).isEqualTo(1);

        // retried every 15 minutes, 4 attempts in total
        ZonedDateTime attempt = first;
        for (int i = 2; i <= 4; i++) {
            attempt = attempt.plusMinutes(15);
            digests.runIfDue(attempt);
            touched(attempt);
            assertThat(repository.findByDay(DAY).orElseThrow().attempts()).isEqualTo(i);
        }
        digests.runIfDue(attempt.plusHours(1));
        assertThat(repository.findByDay(DAY).orElseThrow().attempts()).isEqualTo(4);
        assertThat(FakeNtfy.requests()).hasSize(4);

        // next day it works again
        FakeNtfy.reset();
        LocalDate next = DAY.plusDays(1);
        digests.runIfDue(at(next, 8, 0));
        assertThat(FakeNtfy.requests()).hasSize(1);

        // the scheduler uses the current time; it must not fail
        scheduler.tick();
    }
}
