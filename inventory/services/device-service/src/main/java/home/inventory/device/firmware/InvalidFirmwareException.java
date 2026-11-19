package home.inventory.device.firmware;

/** The uploaded file is not a firmware image of this project. */
public class InvalidFirmwareException extends RuntimeException {

    public InvalidFirmwareException(String message) {
        super(message);
    }
}
