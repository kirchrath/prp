package home.inventory.notification.settings;

import home.inventory.notification.digest.DigestService;
import home.inventory.notification.settings.dto.SettingsDto;
import home.inventory.notification.settings.dto.SettingsSaveDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notification/settings")
@Tag(name = "NotificationSettings")
public class SettingsController {

    private final SettingsService service;
    private final DigestService digests;

    SettingsController(SettingsService service, DigestService digests) {
        this.service = service;
        this.digests = digests;
    }

    @GetMapping
    public SettingsDto getNotificationSettings() {
        return service.get();
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateNotificationSettings(@Valid @RequestBody SettingsSaveDto request) {
        service.update(request);
    }

    @PostMapping("/test")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Sends a test message to the ntfy topic (502 if ntfy cannot be reached)")
    public void sendTestNotification() {
        digests.sendTest();
    }
}
