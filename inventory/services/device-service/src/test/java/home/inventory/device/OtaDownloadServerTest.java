package home.inventory.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import home.inventory.device.config.OtaProperties;
import home.inventory.device.ota.OtaDownloadServer;
import home.inventory.device.ota.OtaService;
import java.io.UncheckedIOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class OtaDownloadServerTest {

    @Test
    void startsAndStopsAndFailsOnAPortInUse() {
        OtaDownloadServer first = new OtaDownloadServer(mock(OtaService.class), new OtaProperties(0, Duration.ofMinutes(15)));
        first.start();
        assertThat(first.isRunning()).isTrue();
        assertThat(first.tokenValidity()).isEqualTo(Duration.ofMinutes(15));

        OtaDownloadServer second = new OtaDownloadServer(mock(OtaService.class),
                new OtaProperties(first.port(), Duration.ofMinutes(15)));
        assertThatThrownBy(second::start).isInstanceOf(UncheckedIOException.class);

        first.stop();
        assertThat(first.isRunning()).isFalse();
    }
}
