package home.inventory.notification.digest;

public enum DigestStatus {
    /** Digest was sent to ntfy. */
    SENT,
    /** Nothing expired or expiring: no message. */
    NOTHING_DUE,
    /** ntfy could not be reached; tried again later (up to the configured number of attempts). */
    FAILED
}
