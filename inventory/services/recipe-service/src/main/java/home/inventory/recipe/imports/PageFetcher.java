package home.inventory.recipe.imports;

import home.inventory.recipe.common.AiException;
import java.net.URI;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Loads recipe pages. */
@Component
public class PageFetcher {

    private final RestClient client;

    public PageFetcher(@Qualifier("pageRestClient") RestClient client) {
        this.client = client;
    }

    public String fetch(URI url) {
        try {
            String body = client.get().uri(url).retrieve().body(String.class);
            if (body == null || body.isBlank()) {
                throw new AiException("Die Seite " + url + " ist leer");
            }
            return body;
        } catch (RestClientException e) {
            throw new AiException("Die Seite " + url + " konnte nicht geladen werden", e);
        }
    }

    /** Visible text of a page (title, meta description, body), shortened to {@code max} characters. */
    public static String text(String html, int max) {
        Document document = Jsoup.parse(html);
        String text = (document.title() + "\n" + document.select("meta[name=description]").attr("content") + "\n"
                + document.body().text()).trim();
        return text.length() > max ? text.substring(0, max) : text;
    }

    /** Image announced for link previews (og:image), if any. */
    public static String previewImage(String html) {
        return Jsoup.parse(html).select("meta[property=og:image]").attr("content");
    }
}
