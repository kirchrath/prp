package home.inventory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

/**
 * Writes the OpenAPI document (code-first via springdoc) to {@code inventory/openapi/openapi.json}.
 * The frontend generates its TypeScript client from this file ({@code npm run generate-api}).
 */
class OpenApiExportTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    void exportOpenApiDocument() throws Exception {
        String raw = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode doc = json.readTree(raw);
        // the generated client uses relative URLs; nginx proxies /api to the backend
        ((tools.jackson.databind.node.ObjectNode) doc).remove("servers");
        String pretty = json.writer().with(SerializationFeature.INDENT_OUTPUT).writeValueAsString(doc);
        Path target = Path.of("..", "openapi", "openapi.json");
        Files.createDirectories(target.getParent());
        Files.writeString(target, pretty + "\n", StandardCharsets.UTF_8);
    }
}
