package home.inventory.device.ota;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import home.inventory.device.config.OtaProperties;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

/**
 * Small HTTP server on its own port (default 8088, reachable in the home network) that only delivers firmware
 * images: {@code GET /ota/<token>}. No TLS needed on the ESP32: the image is only installed if its SHA-256 matches
 * the checksum sent over the authenticated MQTT connection.
 */
@Service
public class OtaDownloadServer implements SmartLifecycle, OtaDownloadPort {

    private static final Logger log = LoggerFactory.getLogger(OtaDownloadServer.class);

    private final OtaService ota;
    private final OtaProperties properties;
    private HttpServer server;

    public OtaDownloadServer(@org.springframework.context.annotation.Lazy OtaService ota, OtaProperties properties) {
        this.ota = ota;
        this.properties = properties;
    }

    @Override
    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(properties.port()), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/", this::handle);
        server.start();
        log.info("Firmware download server on port {}", port());
    }

    void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        Optional<byte[]> image = "GET".equals(exchange.getRequestMethod()) && path.startsWith("/ota/")
                ? ota.download(path.substring("/ota/".length()))
                : Optional.empty();
        if (image.isEmpty()) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
        exchange.sendResponseHeaders(200, image.get().length);
        try (OutputStream body = exchange.getResponseBody()) {
            body.write(image.get());
        }
    }

    @Override
    public void stop() {
        server.stop(0);
        server = null;
    }

    @Override
    public boolean isRunning() {
        return server != null;
    }

    @Override
    public int port() {
        return server.getAddress().getPort();
    }

    @Override
    public Duration tokenValidity() {
        return properties.tokenValidity();
    }
}
