package home.inventory.common;

/** Thrown when a request violates a business rule (e.g. deleting a non-empty location). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
