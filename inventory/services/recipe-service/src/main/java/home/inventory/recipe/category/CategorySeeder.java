package home.inventory.recipe.category;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Creates the default recipe categories on the first start. */
@Component
public class CategorySeeder implements ApplicationRunner {

    private final CategoryService categories;

    public CategorySeeder(CategoryService categories) {
        this.categories = categories;
    }

    @Override
    public void run(ApplicationArguments args) {
        categories.seedDefaults();
    }
}
