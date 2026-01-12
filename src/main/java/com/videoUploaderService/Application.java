package com.videoUploaderService;

import com.videoUploaderService.config.DotenvPropertySourceInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		// Suprimir aviso de deprecação do AWS SDK v1 antes de inicializar o Spring
		System.setProperty("aws.java.v1.disableDeprecationAnnouncement", "true");
		
		SpringApplication app = new SpringApplication(Application.class);
		// Adiciona o inicializador que carrega o .env
		app.addInitializers(new DotenvPropertySourceInitializer());
		app.run(args);
	}

}
