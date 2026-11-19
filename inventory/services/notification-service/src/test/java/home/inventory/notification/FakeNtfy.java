package home.inventory.notification;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Minimal ntfy server for tests: records every request and answers with {@link #status}. */
public final class FakeNtfy {

    public record Request(String path, String authorization, String body) {
    }

    private static final HttpServer SERVER;
    private static final List<Request> REQUESTS = new CopyOnWriteArrayList<>();
    private static volatile int status = 200;

    static {
        try {
            SERVER = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        SERVER.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            REQUESTS.add(new Request(exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Authorization"), body));
            byte[] answer = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, answer.length);
            exchange.getResponseBody().write(answer);
            exchange.close();
        });
        SERVER.start();
    }

    private FakeNtfy() {
    }

    public static String url() {
        return "http://localhost:" + SERVER.getAddress().getPort();
    }

    public static List<Request> requests() {
        return REQUESTS;
    }

    public static void reset() {
        REQUESTS.clear();
        status = 200;
    }

    public static void failWith(int httpStatus) {
        status = httpStatus;
    }
}
