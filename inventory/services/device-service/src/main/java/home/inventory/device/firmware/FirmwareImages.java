package home.inventory.device.firmware;

import home.inventory.device.scanner.DeviceKind;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks an uploaded ESP32 image. Every firmware of this project contains the marker
 * {@code INVFW|<kind>|<version>|} (see the firmware sources), so kind and version are read from the image
 * and a button box image can never be installed on a scanner.
 */
public final class FirmwareImages {

    /** Size of an app partition with the partition table min_spiffs.csv (two OTA slots). */
    public static final int MAX_SIZE = 0x1E0000;
    private static final byte ESP_IMAGE_MAGIC = (byte) 0xE9;
    private static final Pattern MARKER = Pattern.compile("INVFW\\|(scanner|buttons)\\|([0-9A-Za-z.+_-]{1,32})\\|");

    public record Info(DeviceKind kind, String version, String sha256) {
    }

    private FirmwareImages() {
    }

    public static Info inspect(byte[] image) {
        if (image.length == 0 || image[0] != ESP_IMAGE_MAGIC) {
            throw new InvalidFirmwareException("Keine ESP32-Firmware (.bin aus .pio/build/<env>/firmware.bin hochladen)");
        }
        if (image.length > MAX_SIZE) {
            throw new InvalidFirmwareException("Firmware ist größer als die OTA-Partition (" + MAX_SIZE + " Bytes)");
        }
        // ISO-8859-1 maps every byte to one char, so the marker can be searched as text
        Matcher m = MARKER.matcher(new String(image, StandardCharsets.ISO_8859_1));
        if (!m.find()) {
            throw new InvalidFirmwareException("Firmware-Kennung fehlt – nur Firmware dieses Projekts kann installiert werden");
        }
        DeviceKind kind = "buttons".equals(m.group(1)) ? DeviceKind.BUTTONS : DeviceKind.SCANNER;
        return new Info(kind, m.group(2), sha256(image));
    }

    static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
