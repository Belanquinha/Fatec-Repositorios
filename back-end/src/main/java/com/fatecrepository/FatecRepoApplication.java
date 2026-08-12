package com.fatecrepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
@SpringBootApplication
public class FatecRepoApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(FatecRepoApplication.class, args);
		log.info("Aplicação FatecRepository iniciada com sucesso");
		log.info("Swagger UI disponível em: http://localhost:4040/swagger-ui.html");
		log.info("API Docs disponível em: http://localhost:4040/v3/api-docs");
	}

}
