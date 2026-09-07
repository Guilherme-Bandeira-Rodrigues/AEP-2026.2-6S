package com.gbr.AEP.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI projetoSocialOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Projetos Sociais")
                        .description("API REST para cadastro e gerenciamento de projetos sociais.")
                        .version("v1")
                        .contact(new Contact().name("Equipe AEP")));
    }
}
