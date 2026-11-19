package home.inventory.recipe.common;

/** The AI provider or a recipe page could not be used. */
public class AiException extends RuntimeException {

    public AiException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiException(String message) {
        super(message);
    }
}
