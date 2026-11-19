package home.inventory.device.ota;

import java.time.Duration;

/** Where devices download firmware images (port of the download server) and how long a token is valid. */
public interface OtaDownloadPort {

    int port();

    Duration tokenValidity();
}
