package home.inventory.device;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.node.ObjectNode;

/** Writes the OpenAPI document to inventory/openapi/device.json; the frontend generates its client from it. */
class OpenApiExportTest extends AbstractIntegrationTest {

    @Autowired
    ObjectMapper json;

    @Test
    void exportOpenApiDocument() throws Exception {
        String raw = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        ObjectNode doc = (ObjectNode) json.readTree(raw);
        doc.remove("servers");
        Path target = Path.of("..", "..", "openapi", "device.json");
        Files.createDirectories(target.getParent());
        Files.writeString(target, json.writer().with(SerializationFeature.INDENT_OUTPUT).writeValueAsString(doc) + "\n");
    }
}
