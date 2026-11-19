package home.inventory.device.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param port          port of the firmware download server (only {@code /ota/<token>}); 0 = random (tests)
 * @param tokenValidity how long a device may download and install after the update was started
 */
@ConfigurationProperties("device.ota")
public record OtaProperties(@DefaultValue("8088") int port, @DefaultValue("15m") Duration tokenValidity) {
}
