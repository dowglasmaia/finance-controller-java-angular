package com.dowglasmaia.financecontroller.support;

import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Real SMTP capture server (Mailpit) shared by the test run: the application sends through SMTP and the tests read the
 * captured messages over Mailpit's HTTP API. Nothing is mocked on the mail path.
 */
public final class MailpitSupport {
    private static final GenericContainer<?> MAILPIT = new GenericContainer<>(DockerImageName.parse("axllent/mailpit:v1.21"))
        .withExposedPorts(1025, 8025)
        .waitingFor(Wait.forListeningPort());
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    static {
        MAILPIT.start();
    }

    private MailpitSupport() {
    }

    public static String smtpHost() {
        return MAILPIT.getHost();
    }

    public static int smtpPort() {
        return MAILPIT.getMappedPort(1025);
    }

    /** Subject, text and HTML of every message captured for {@code to} (oldest first). */
    public static List<Captured> messagesTo(String to) throws Exception {
        String list = get("/api/v1/messages?limit=500");
        List<String> ids = JsonPath.read(list, "$.messages[?(@.To[0].Address=='" + to.toLowerCase(Locale.ROOT) + "')].ID");
        return ids.reversed().stream().map(id -> {
            try {
                String message = get("/api/v1/message/" + id);
                return new Captured(JsonPath.read(message, "$.Subject"), JsonPath.read(message, "$.Text"),
                    JsonPath.read(message, "$.HTML"), JsonPath.read(message, "$.From.Address"));
            } catch (Exception failure) {
                throw new IllegalStateException(failure);
            }
        }).toList();
    }

    private static String get(String path) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025) + path)).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    public record Captured(String subject, String text, String html, String from) {
    }
}
