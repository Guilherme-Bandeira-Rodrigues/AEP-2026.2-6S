package com.gbr.AEP.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer segurancaDosEndpoints() {
        return api -> api.getPaths().forEach((caminho, item) -> item.readOperationsMap().forEach((metodo, operacao) -> {
            if (!caminho.startsWith("/api/")) {
                return;
            }
            boolean escrita = List.of(PathItem.HttpMethod.POST, PathItem.HttpMethod.PUT,
                    PathItem.HttpMethod.DELETE, PathItem.HttpMethod.PATCH).contains(metodo);
            boolean entrada = caminho.equals("/api/auth/login") || caminho.equals("/api/auth/cadastro");
            boolean autenticado = (escrita && !entrada) || caminho.startsWith("/api/me/")
                    || caminho.equals("/api/auth/me");
            SecurityRequirement requisito = new SecurityRequirement();
            if (escrita) {
                requisito.addList("csrf");
            }
            if (autenticado) {
                requisito.addList("sessao");
            }
            if (!requisito.isEmpty()) {
                operacao.setSecurity(List.of(requisito));
            }
        }));
    }

    @Bean
    public OpenAPI projetoSocialOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Projetos Sociais")
                        .description("Consultas públicas e gestão por proprietário. Obtenha o token em GET /api/auth/csrf "
                                + "e envie X-CSRF-TOKEN em POST/PUT/DELETE, inclusive cadastro, login e logout. "
                                + "Após login, obtenha um novo token. Autenticação por cookie de sessão HttpOnly.")
                        .version("v2")
                        .contact(new Contact().name("Equipe AEP")))
                .components(new Components()
                        .addSecuritySchemes("sessao", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE).name("JSESSIONID"))
                        .addSecuritySchemes("csrf", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER).name("X-CSRF-TOKEN")))
                // Logout é executado pelo filtro de segurança, sem controller MVC.
                .path("/api/auth/logout", new PathItem().post(new Operation().summary("Encerrar sessão")
                        .addTagsItem("Autenticação")
                        .addSecurityItem(new SecurityRequirement().addList("sessao").addList("csrf"))
                        .responses(new ApiResponses().addApiResponse("204", new ApiResponse().description("Sessão encerrada"))
                                .addApiResponse("403", new ApiResponse().description("Token CSRF ausente ou inválido")))));
    }
}
