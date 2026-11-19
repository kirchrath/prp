package home.inventory.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import home.inventory.device.firmware.FirmwareImages;
import home.inventory.device.firmware.InvalidFirmwareException;
import home.inventory.device.scanner.DeviceKind;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class FirmwareImagesTest {

    /** Minimal fake ESP32 image: magic byte, some binary, the project marker. */
    static byte[] image(String marker) {
        byte[] text = ("\u0001\u0002binary" + marker + "more").getBytes(StandardCharsets.ISO_8859_1);
        byte[] image = new byte[text.length + 1];
        image[0] = (byte) 0xE9;
        System.arraycopy(text, 0, image, 1, text.length);
        return image;
    }

    @Test
    void readsKindAndVersionFromTheMarker() {
        FirmwareImages.Info scanner = FirmwareImages.inspect(image("INVFW|scanner|0.3.0|"));
        assertThat(scanner.kind()).isEqualTo(DeviceKind.SCANNER);
        assertThat(scanner.version()).isEqualTo("0.3.0");
        assertThat(scanner.sha256()).hasSize(64);
        assertThat(FirmwareImages.inspect(image("INVFW|buttons|1.0.0-rc1|")).kind()).isEqualTo(DeviceKind.BUTTONS);
    }

    @Test
    void failsWithoutSha256() {
        try (var digest = org.mockito.Mockito.mockStatic(java.security.MessageDigest.class)) {
            digest.when(() -> java.security.MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new java.security.NoSuchAlgorithmException("none"));
            assertThatThrownBy(() -> FirmwareImages.inspect(image("INVFW|scanner|1.0|")))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void rejectsOtherFiles() {
        assertThatThrownBy(() -> FirmwareImages.inspect(new byte[0])).isInstanceOf(InvalidFirmwareException.class)
                .hasMessageStartingWith("Keine ESP32-Firmware");
        assertThatThrownBy(() -> FirmwareImages.inspect("INVFW|scanner|1|".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(InvalidFirmwareException.class);
        assertThatThrownBy(() -> FirmwareImages.inspect(image("INVFW|toaster|1.0|")))
                .hasMessageStartingWith("Firmware-Kennung fehlt");
        byte[] huge = new byte[FirmwareImages.MAX_SIZE + 1];
        huge[0] = (byte) 0xE9;
        assertThatThrownBy(() -> FirmwareImages.inspect(huge)).hasMessageContaining("OTA-Partition");
    }
}
