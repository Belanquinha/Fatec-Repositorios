package com.fatecrepository.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI customOpenAPI() {
		log.info("Configurando OpenAPI/Swagger");
		
		return new OpenAPI()
			.servers(List.of(
				new Server().url("http://localhost:4040").description("Development Server"),
				new Server().url("https://api.fatecrepository.com").description("Production Server")
			))
			.info(new Info()
				.title("FATEC Repository API")
				.version("1.0.0")
				.description("API para gerenciamento da FATEC")
				.contact(new Contact()
					.name("FATEC Repository Team")
					.email("contact@fatecrepository.com")
				)
				.license(new License()
					.name("MIT License")
					.url("https://opensource.org/licenses/MIT")
				)
			)
			.addSecurityItem(new SecurityRequirement().addList("Bearer JWT"))
			.components(new io.swagger.v3.oas.models.Components()
				.addSecuritySchemes("Bearer JWT", new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")
					.description("Enter JWT token")
				)
			);
	}
}
