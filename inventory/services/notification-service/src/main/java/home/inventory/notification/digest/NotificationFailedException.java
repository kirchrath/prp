package home.inventory.notification.digest;

/** ntfy could not be reached or rejected the message. */
public class NotificationFailedException extends RuntimeException {

    public NotificationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
