package com.gbr.AEP;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.address=127.0.0.1", "server.servlet.session.cookie.secure=false"})
class HttpSecurityIntegrationTest {
    @Value("${local.server.port}")
    private int porta;

    @Test
    void cookieRealEhHttpOnlySameSiteEEscritasAnonimasSaoNegadas() throws Exception {
        try (HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager())
                .connectTimeout(Duration.ofSeconds(5)).build()) {
            var csrf = client.send(requisicao("/api/auth/csrf").GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(csrf.statusCode()).isEqualTo(200);
            assertThat(csrf.headers().firstValue("set-cookie").orElseThrow())
                    .contains("JSESSIONID=", "HttpOnly", "SameSite=Lax");
            String token = JsonPath.read(csrf.body(), "$.token");
            var escrita = client.send(requisicao("/api/organizacoes")
                    .header("X-CSRF-TOKEN", token).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}" )).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(escrita.statusCode()).isEqualTo(401);
            assertThat(JsonPath.<Integer>read(escrita.body(), "$.status")).isEqualTo(401);
            var semCsrf = client.send(requisicao("/api/auth/login").header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}" )).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(semCsrf.statusCode()).isEqualTo(403);
        }
    }

    @Test
    void swaggerDocumentaLogoutERequisitosReaisSemProtegerConsultasPublicas() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            var resposta = client.send(requisicao("/v3/api-docs").GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(resposta.statusCode()).isEqualTo(200);
            var api = JsonPath.parse(resposta.body());
            Map<String, Object> escrita = api.read("$.paths['/api/projetos'].post.security[0]");
            assertThat(escrita).containsKeys("csrf", "sessao");
            Map<String, Object> login = api.read("$.paths['/api/auth/login'].post.security[0]");
            assertThat(login).containsKey("csrf").doesNotContainKey("sessao");
            Map<String, Object> consulta = api.read("$.paths['/api/projetos'].get");
            assertThat(consulta).doesNotContainKey("security");
            List<Map<String, Object>> privado = api.read("$.paths['/api/me/projetos'].get.security");
            assertThat(privado.getFirst()).containsKey("sessao");
            assertThat(api.<Map<String, Object>>read("$.paths['/api/auth/logout'].post.responses"))
                    .containsKey("204");
        }
    }

    private HttpRequest.Builder requisicao(String caminho) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + caminho))
                .timeout(Duration.ofSeconds(15));
    }
}
