package home.inventory.recipe.imports;

import home.inventory.recipe.common.AiException;
import home.inventory.recipe.copy.ArticleCopy;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

/** Brings recipes into structure with the configured AI provider (Spring AI). */
@Service
public class RecipeAiService {

    static final String RULES = """
            Du bist ein Küchenassistent und überführst Rezepte in eine feste Struktur.
            Regeln für Zutaten:
            - name: nur die Zutat, ohne Menge und Einheit, auf Deutsch.
            - amount/unit: Menge als Zahl; Einheiten GRAM, KILOGRAM, MILLILITER, LITER, PIECE (Stück),
              PACKAGE (Packung/Dose/Becher), TABLESPOON (EL), TEASPOON (TL), PINCH (Prise), OTHER (sonstige).
              Brüche als Dezimalzahl (1/2 = 0.5). Ohne Mengenangabe: amount und unit leer.
            - basic: true nur für Grundzutaten, die man immer im Haus hat (Salz, Pfeffer, Wasser, Öl, Zucker).
            - articleId: die id des passenden Artikels aus der Artikelliste, wenn es eindeutig derselbe Lebensmitteltyp ist;
              sonst leer. Erfinde keine ids.
            Übernimm nur Informationen aus dem Text, erfinde nichts hinzu.
            """;

    private final ChatClient chat;

    public RecipeAiService(ChatClient chat) {
        this.chat = chat;
    }

    /** Complete recipe from a free text or the text of a web page. */
    public AiRecipe extractRecipe(String text, List<String> categories, List<ArticleCopy> articles) {
        String user = "Kategorien: " + String.join(", ", categories) + "\n\nArtikelliste:\n" + articleList(articles)
                + "\n\nRezepttext:\n" + text;
        return ask(user, AiRecipe.class);
    }

    /** Ingredient lines of a recipe page (schema.org data) structured and matched to articles. */
    public List<AiIngredient> structureIngredients(List<String> lines, List<ArticleCopy> articles) {
        String user = "Artikelliste:\n" + articleList(articles) + "\n\nZutaten (eine je Zeile, Reihenfolge beibehalten):\n"
                + String.join("\n", lines);
        return ask(user, AiIngredients.class).ingredients();
    }

    private <T> T ask(String user, Class<T> type) {
        T result;
        try {
            result = chat.prompt(new Prompt(List.of(new SystemMessage(RULES), new UserMessage(user)))).call().entity(type);
        } catch (RuntimeException e) {
            throw new AiException("Die KI-Anfrage ist fehlgeschlagen: " + e.getMessage(), e);
        }
        if (result == null) {
            throw new AiException("Die KI hat keine verwertbare Antwort geliefert");
        }
        return result;
    }

    static String articleList(List<ArticleCopy> articles) {
        return articles.stream()
                .map(a -> a.articleId() + " | " + a.name() + (a.brand() == null ? "" : " (" + a.brand() + ")"))
                .collect(Collectors.joining("\n"));
    }
}
