package home.inventory.recipe.imports;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Recipe as extracted by the AI from a free text or a web page. */
public record AiRecipe(
        @JsonPropertyDescription("Name des Rezepts") String title,
        @JsonPropertyDescription("Kurze Beschreibung") @Nullable String description,
        @JsonPropertyDescription("Anzahl Portionen/Personen, für die die Mengen gelten") @Nullable Integer servings,
        @JsonPropertyDescription("Vorbereitungszeit in Minuten") @Nullable Integer prepMinutes,
        @JsonPropertyDescription("Koch-/Backzeit in Minuten") @Nullable Integer cookMinutes,
        @JsonPropertyDescription("Eine der vorgegebenen Kategorien") @Nullable String category,
        @JsonPropertyDescription("Schlagworte, z. B. vegetarisch, schnell") @Nullable List<String> tags,
        @JsonPropertyDescription("Zubereitungsschritte in Reihenfolge") @Nullable List<String> steps,
        @Nullable List<AiIngredient> ingredients) {
}
