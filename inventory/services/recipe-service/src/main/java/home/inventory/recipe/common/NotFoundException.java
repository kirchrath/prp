package home.inventory.recipe.common;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String entity, Object id) {
        super(entity + " " + id + " nicht gefunden");
    }
}
