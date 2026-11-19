package home.inventory.recipe.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI inventoryOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Recipe API").version("v1"))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }

    /**
     * Marks every record component as required unless it is annotated with JSpecify's {@link Nullable}.
     * This makes the generated TypeScript client precise ({@code name: string} vs. {@code brand?: string}).
     */
    @Bean
    OpenApiCustomizer requiredRecordComponents() {
        Map<String, Class<?>> records = findRecords();
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            openApi.getComponents().getSchemas().forEach((name, schema) -> {
                Class<?> type = records.get(name);
                if (type != null) {
                    markRequired(schema, type);
                }
            });
        };
    }

    private static void markRequired(Schema<?> schema, Class<?> type) {
        if (schema.getProperties() == null) {
            return;
        }
        List<String> required = new ArrayList<>();
        for (RecordComponent component : type.getRecordComponents()) {
            boolean nullable = component.getAnnotatedType().isAnnotationPresent(Nullable.class)
                    || component.isAnnotationPresent(Nullable.class);
            if (!nullable && schema.getProperties().containsKey(component.getName())) {
                required.add(component.getName());
            }
        }
        schema.setRequired(required.isEmpty() ? null : required);
    }

    private static Map<String, Class<?>> findRecords() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Record.class));
        Map<String, Class<?>> result = new HashMap<>();
        for (var candidate : scanner.findCandidateComponents("home.inventory.recipe")) {
            Class<?> type = ClassUtils.resolveClassName(candidate.getBeanClassName(), OpenApiConfig.class.getClassLoader());
            result.put(type.getSimpleName(), type);
        }
        return result;
    }
}
