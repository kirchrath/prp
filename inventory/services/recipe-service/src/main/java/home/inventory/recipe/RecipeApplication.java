package home.inventory.recipe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RecipeApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecipeApplication.class, args);
    }
}
