package com.dowglasmaia.financecontroller.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dowglasmaia.financecontroller.Application;
import com.dowglasmaia.financecontroller.support.PostgresContainerConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The production profile (F12): refuses to start without its settings; with them it runs as a DML-only database user while
 * Flyway migrates as the owner; hides internals; turns readiness DOWN when the database disappears.
 */
class ProductionProfileIntegrationTest {
    private static final PostgreSQLContainer PG = PostgresContainerConfiguration.container();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Test
    void doesNotStartWithoutTheRequiredSettings() {
        var failure = assertThrows(Exception.class, () -> new SpringApplicationBuilder(Application.class)
            .profiles("prod")
            .run("--server.port=0", "--spring.main.banner-mode=off"));

        String message = rootMessages(failure);
        assertTrue(message.contains("Could not resolve placeholder"), message);
        assertTrue(message.contains("FC_MAIL_HOST") || message.contains("SPRING_DATASOURCE_URL") || message.contains("FC_APP_BASE_URL"), message);
    }

    @Test
    void doesNotStartWithAPlainHttpBaseUrl() throws Exception {
        String database = createDatabase();
        String appPassword = "app-" + UUID.randomUUID();
        runRolesScript(database, appPassword);
        var failure = assertThrows(Exception.class, () -> start(database, "http://app.example.com", appPassword));
        assertTrue(rootMessages(failure).contains("must be https"), rootMessages(failure));
    }

    @Test
    void runsAsADmlOnlyUserWhileFlywayMigratesAsTheOwnerAndHidesInternals() throws Exception {
        String database = createDatabase();
        String appPassword = "app-" + UUID.randomUUID();
        runRolesScript(database, appPassword);

        try (ConfigurableApplicationContext context = start(database, "https://app.example.com", appPassword)) {
            int port = Integer.parseInt(context.getEnvironment().getProperty("local.server.port"));
            String base = "http://localhost:" + port;
            Environment env = context.getEnvironment();
            assertEquals("true", env.getProperty("app.identity.cookie.secure"));

            // cadastro real: a aplicação (fc_app) grava e lê
            String email = "prod." + UUID.randomUUID() + "@example.com";
            var register = post(base + "/api/v1/auth/register",
                "{\"email\":\"" + email + "\",\"password\":\"Sup3r-Secret-Passw0rd\"}");
            assertEquals(201, register.statusCode(), register.body());
            assertTrue(register.headers().firstValue("Set-Cookie").orElse("").contains("Secure"), "cookie de renovação Secure");

            // erro: nada de pilha, SQL ou nomes de classe
            var broken = post(base + "/api/v1/auth/login", "{not json");
            assertEquals(400, broken.statusCode());
            for (String leak : new String[] {"Exception", "at com.", "org.springframework", "SELECT ", "stackTrace"}) {
                assertFalse(broken.body().contains(leak), leak + " em " + broken.body());
            }

            // saúde sem detalhes
            var health = get(base + "/actuator/health");
            assertEquals(200, health.statusCode());
            assertFalse(health.body().contains("\"db\""), health.body());
            assertFalse(health.body().contains("jdbc"), health.body());
            assertEquals(200, get(base + "/actuator/health/readiness").statusCode());

            // o utilizador da aplicação não tem DDL; DML funciona
            try (Connection app = connect(database, "fc_app", appPassword); Statement st = app.createStatement()) {
                assertThrows(SQLException.class, () -> st.execute("CREATE TABLE intruso (id int)"));
                assertThrows(SQLException.class, () -> st.execute("DROP TABLE app_user"));
                assertThrows(SQLException.class, () -> st.execute("ALTER TABLE app_user ADD COLUMN x int"));
                assertThrows(SQLException.class, () -> st.execute("TRUNCATE app_user"));
                assertTrue(st.executeQuery("SELECT count(*) FROM app_user").next());
            }

            // sem banco, a API sai de produção: pronto = DOWN (503) e a saúde geral também
            try (Connection owner = connect("postgres", PG.getUsername(), PG.getPassword()); Statement st = owner.createStatement()) {
                st.execute("DROP DATABASE " + database + " WITH (FORCE)");
            }
            var readiness = get(base + "/actuator/health/readiness");
            assertEquals(503, readiness.statusCode(), readiness.body());
            assertTrue(readiness.body().contains("DOWN"), readiness.body());
            assertEquals(503, get(base + "/actuator/health").statusCode());
        }
    }

    // ------------------------------------------------------------------ apoio

    private static ConfigurableApplicationContext start(String database, String baseUrl, String appPassword) {
        String url = "jdbc:postgresql://" + PG.getHost() + ":" + PG.getMappedPort(5432) + "/" + database;
        return new SpringApplicationBuilder(Application.class)
            .profiles("prod")
            .run(
                "--server.port=0", "--spring.main.banner-mode=off",
                "--spring.datasource.url=" + url, "--spring.datasource.username=fc_app", "--spring.datasource.password=" + appPassword,
                "--spring.datasource.hikari.connection-timeout=2000",
                "--spring.flyway.user=" + PG.getUsername(), "--spring.flyway.password=" + PG.getPassword(),
                "--app.identity.jwt.secret=test-only-dummy-jwt-secret-0123456789-abcdef",
                "--app.identity.cors.allowed-origins=https://app.example.com",
                "--app.password-reset.app-base-url=" + baseUrl, "--app.password-reset.mail-from=no-reply@example.com",
                "--spring.mail.host=localhost", "--spring.mail.port=1");
    }

    private static String createDatabase() throws Exception {
        String name = "fc_prod_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        try (Connection owner = connect("postgres", PG.getUsername(), PG.getPassword()); Statement st = owner.createStatement()) {
            st.execute("CREATE DATABASE " + name + " OWNER " + PG.getUsername());
        }
        return name;
    }

    /** Executa o script versionado de papéis, como o dono do banco, com o utilizador `fc_app` único por banco. */
    private static void runRolesScript(String database, String appPassword) throws Exception {
        String script = Files.readString(Path.of("..", "scripts", "db", "prod-roles.sql"))
            .replace("__APP_PASSWORD__", appPassword);
        try (Connection owner = connect(database, PG.getUsername(), PG.getPassword()); Statement st = owner.createStatement()) {
            // fc_app é de cluster inteiro: recria para este teste
            st.execute("DROP OWNED BY fc_app");
        } catch (SQLException noRoleYet) {
            // primeira execução: o papel ainda não existe
        }
        try (Connection owner = connect("postgres", PG.getUsername(), PG.getPassword()); Statement st = owner.createStatement()) {
            st.execute("DROP ROLE IF EXISTS fc_app");
        } catch (SQLException stillOwnsObjects) {
            // outro banco de teste ainda o referencia: a senha é trocada abaixo
        }
        try (Connection owner = connect(database, PG.getUsername(), PG.getPassword()); Statement st = owner.createStatement()) {
            try {
                st.execute(script);
            } catch (SQLException roleExists) {
                st.execute("ALTER ROLE fc_app PASSWORD '" + appPassword + "'");
                String withoutCreateRole = script.replaceFirst("CREATE ROLE[^;]*;", "");
                st.execute(withoutCreateRole);
            }
        }
    }

    private static Connection connect(String database, String user, String password) throws SQLException {
        return DriverManager.getConnection("jdbc:postgresql://" + PG.getHost() + ":" + PG.getMappedPort(5432) + "/" + database,
            user, password);
    }

    private static HttpResponse<String> get(String url) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> post(String url, String body) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String rootMessages(Throwable failure) {
        StringBuilder all = new StringBuilder();
        for (Throwable t = failure; t != null; t = t.getCause()) {
            all.append(t.getMessage()).append(" | ");
        }
        return all.toString();
    }
}
