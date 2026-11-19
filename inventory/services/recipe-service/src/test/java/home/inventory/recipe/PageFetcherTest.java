package home.inventory.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import home.inventory.recipe.common.AiException;
import home.inventory.recipe.imports.PageFetcher;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PageFetcherTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final PageFetcher fetcher = new PageFetcher(builder.build());

    @Test
    void loadsPages() {
        server.expect(requestTo("https://rezepte.test/a")).andRespond(withSuccess("<html>ok</html>", MediaType.TEXT_HTML));
        assertThat(fetcher.fetch(URI.create("https://rezepte.test/a"))).isEqualTo("<html>ok</html>");
    }

    @Test
    void reportsEmptyAndFailingPages() {
        server.expect(requestTo("https://rezepte.test/empty")).andRespond(withSuccess(" ", MediaType.TEXT_HTML));
        server.expect(requestTo("https://rezepte.test/none")).andRespond(withSuccess());
        server.expect(requestTo("https://rezepte.test/error")).andRespond(withServerError());
        assertThatThrownBy(() -> fetcher.fetch(URI.create("https://rezepte.test/empty"))).isInstanceOf(AiException.class).hasMessageContaining("leer");
        assertThatThrownBy(() -> fetcher.fetch(URI.create("https://rezepte.test/none"))).isInstanceOf(AiException.class);
        assertThatThrownBy(() -> fetcher.fetch(URI.create("https://rezepte.test/error"))).isInstanceOf(AiException.class)
                .hasMessageContaining("nicht geladen");
    }

    @Test
    void extractsTextAndPreviewImage() {
        String html = "<html><head><title>Kuchen</title><meta name=\"description\" content=\"lecker\">"
                + "<meta property=\"og:image\" content=\"https://img/k.jpg\"></head><body><p>200 g Mehl</p></body></html>";
        assertThat(PageFetcher.text(html, 1000)).isEqualTo("Kuchen\nlecker\n200 g Mehl");
        assertThat(PageFetcher.text(html, 6)).isEqualTo("Kuchen");
        assertThat(PageFetcher.previewImage(html)).isEqualTo("https://img/k.jpg");
        assertThat(PageFetcher.previewImage("<html></html>")).isEmpty();
    }
}
