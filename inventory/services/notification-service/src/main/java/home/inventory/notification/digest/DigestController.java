package home.inventory.notification.digest;

import home.inventory.notification.digest.dto.DigestListDto;
import home.inventory.notification.digest.dto.DueDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notification")
@Tag(name = "Digests")
public class DigestController {

    private final DueService due;
    private final DigestService digests;
    private final Clock clock;

    DigestController(DueService due, DigestService digests, Clock clock) {
        this.due = due;
        this.digests = digests;
        this.clock = clock;
    }

    @GetMapping("/due")
    @Operation(summary = "What the daily digest contains right now")
    public DueDto getDue() {
        return due.due(LocalDate.now(clock));
    }

    @GetMapping("/digests")
    @Operation(summary = "Send log of the last 14 days")
    public List<DigestListDto> listDigests() {
        return digests.recent();
    }
}
